package dev.meteor.payment.service;

import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.order.mapper.OrderMapper;
import dev.meteor.order.mapper.projection.OrderSnapshotProjection;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.payment.dto.PaymentResponse;
import dev.meteor.payment.dto.TestPaymentEventRequest;
import dev.meteor.payment.mapper.PaymentMapper;
import dev.meteor.payment.mapper.param.PaymentAttemptInsert;
import dev.meteor.payment.mapper.param.PaymentEventInsert;
import dev.meteor.payment.mapper.projection.PaymentAttemptProjection;
import dev.meteor.payment.mapper.projection.PaymentEventProjection;
import dev.meteor.payment.model.PaymentAttemptStatus;
import dev.meteor.payment.model.PaymentEventProcessingStatus;
import dev.meteor.payment.model.PaymentEventType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅处理 LOCAL_TEST 成功事件；支付事实、订单状态和购买权益在同一个数据库事务中提交。 */
@Service
public class PaymentService {

    public static final String PROVIDER = "LOCAL_TEST";

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final EntitlementMapper entitlementMapper;

    public PaymentService(
            OrderMapper orderMapper,
            PaymentMapper paymentMapper,
            EntitlementMapper entitlementMapper
    ) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.entitlementMapper = entitlementMapper;
    }

    @Transactional
    public PaymentResponse applyTestPayment(
            long userId,
            long orderId,
            TestPaymentEventRequest request
    ) {
        // 先验证订单归属并锁住订单，串行处理同一订单的支付；全局事件／交易仍由唯一约束兜底。
        OrderSnapshotProjection order = orderMapper.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(orderId));

        PaymentEventProjection existingEvent = paymentMapper
                .findEventByProviderEventId(PROVIDER, request.providerEventId())
                .orElse(null);
        if (existingEvent != null) {
            // 同事件重放也要复核交易、金额与已成功状态，不能只因为事件 ID 存在就返回成功。
            PaymentAttemptProjection eventAttempt = paymentMapper
                    .findAttemptByProviderTransactionId(PROVIDER, existingEvent.providerTransactionId())
                    .orElseThrow(() -> new PaymentConflictException("Payment event has no payment attempt"));
            if (eventAttempt.orderId() != orderId) {
                throw new PaymentConflictException("Payment event is already linked to another order");
            }
            if (!existingEvent.providerTransactionId().equals(request.providerTransactionId())) {
                throw new PaymentConflictException("Provider event ID was reused for another transaction");
            }
            validatePaymentAmount(existingEvent.amountCents(), existingEvent.currency(), request);
            validateReplayableEvent(existingEvent, eventAttempt);
            EntitlementGrantProjection grant = ensureEntitlement(orderId, userId, order.status());
            return toResponse(existingEvent, orderId, order.status(), grant);
        }

        PaymentAttemptProjection transactionAttempt = paymentMapper
                .findAttemptByProviderTransactionId(PROVIDER, request.providerTransactionId())
                .orElse(null);
        if (transactionAttempt != null && transactionAttempt.orderId() != orderId) {
            throw new PaymentConflictException("Payment transaction is already linked to another order");
        }

        PaymentEventProjection existingTransaction = paymentMapper
                .findEventByProviderTransactionId(PROVIDER, request.providerTransactionId())
                .orElse(null);
        if (existingTransaction != null) {
            // 同一交易可能携带另一事件 ID：返回已落库事实，不再记录第二次支付或发第二份权益。
            validatePaymentAmount(existingTransaction.amountCents(), existingTransaction.currency(), request);
            if (transactionAttempt == null) {
                throw new PaymentConflictException("Payment event has no payment attempt");
            }
            validateReplayableEvent(existingTransaction, transactionAttempt);
            EntitlementGrantProjection grant = ensureEntitlement(orderId, userId, order.status());
            return toResponse(existingTransaction, orderId, order.status(), grant);
        }

        // 首次支付按成交快照核对；浏览器传来的金额只是待验证输入，不是定价依据。
        validatePaymentAmount(order.totalAmountCents(), order.currency(), request);
        if (!order.status().acceptsPayment()) {
            throw new PaymentConflictException("Order " + orderId + " is already " + order.status());
        }

        PaymentAttemptProjection attempt = paymentMapper.findAttemptByOrderId(orderId).orElse(null);
        if (attempt != null) {
            if (!request.providerTransactionId().equals(attempt.providerTransactionId())) {
                throw new PaymentConflictException(
                        "Order " + orderId + " already has a different payment transaction"
                );
            }
            if (!attempt.status().acceptsSuccess()) {
                throw new PaymentConflictException("Payment attempt is already " + attempt.status());
            }
        } else {
            PaymentAttemptInsert newAttempt = new PaymentAttemptInsert(
                    orderId,
                    PROVIDER,
                    request.providerTransactionId(),
                    PaymentAttemptStatus.PENDING,
                    order.totalAmountCents(),
                    order.currency()
            );
            try {
                paymentMapper.insertAttempt(newAttempt);
            } catch (DuplicateKeyException duplicateKeyException) {
                PaymentAttemptProjection conflictingAttempt = paymentMapper
                        .findAttemptByProviderTransactionId(PROVIDER, request.providerTransactionId())
                        .orElse(null);
                if (conflictingAttempt != null && conflictingAttempt.orderId() != orderId) {
                    throw new PaymentConflictException(
                            "Payment transaction is already linked to another order"
                    );
                }
                throw duplicateKeyException;
            }
            attempt = new PaymentAttemptProjection(
                    newAttempt.getId(),
                    orderId,
                    PROVIDER,
                    request.providerTransactionId(),
                    PaymentAttemptStatus.PENDING,
                    order.totalAmountCents(),
                    order.currency()
            );
        }

        PaymentEventInsert event = new PaymentEventInsert(
                attempt.id(),
                PROVIDER,
                request.providerEventId(),
                request.providerTransactionId(),
                PaymentEventType.PAYMENT_SUCCEEDED,
                request.amountCents(),
                request.currency(),
                PaymentEventProcessingStatus.APPLIED
        );
        try {
            paymentMapper.insertEvent(event);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new PaymentConflictException("Payment event or transaction was already recorded");
        }
        // SQL 同时限定旧状态，且必须更新一行；任一步失败都会回滚事件、订单与权益。
        if (paymentMapper.markAttemptSucceeded(attempt.id()) != 1) {
            throw new PaymentConflictException("Payment attempt could not transition to SUCCEEDED");
        }
        if (orderMapper.markPaid(orderId, userId) != 1) {
            throw new PaymentConflictException("Order " + orderId + " could not transition to PAID");
        }

        EntitlementGrantProjection grant = ensureEntitlement(orderId, userId, OrderStatus.PAID);

        return new PaymentResponse(
                event.getId(),
                PROVIDER,
                request.providerEventId(),
                request.providerTransactionId(),
                PaymentEventProcessingStatus.APPLIED,
                orderId,
                OrderStatus.PAID,
                request.amountCents(),
                request.currency(),
                grant.id(),
                grant.status()
        );
    }

    private EntitlementGrantProjection ensureEntitlement(
            long orderId,
            long userId,
            OrderStatus orderStatus
    ) {
        if (!orderStatus.hasPaid()) {
            return null;
        }
        // 权益唯一来源是订单项。重复插入只保留旧行，不把已撤销权益重新激活。
        entitlementMapper.ensureGrantForPaidOrder(orderId, userId);
        return entitlementMapper.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Paid order " + orderId + " has no entitlement grant"
                ));
    }

    private void validatePaymentAmount(
            long expectedAmountCents,
            String expectedCurrency,
            TestPaymentEventRequest request
    ) {
        if (expectedAmountCents != request.amountCents()
                || !expectedCurrency.equals(request.currency())) {
            throw new PaymentConflictException("Payment amount or currency does not match the order");
        }
    }

    private void validateReplayableEvent(
            PaymentEventProjection event,
            PaymentAttemptProjection attempt
    ) {
        if (event.eventType() != PaymentEventType.PAYMENT_SUCCEEDED
                || !event.processingStatus().wasApplied()
                || !attempt.status().hasSucceeded()) {
            throw new PaymentConflictException("Payment event was not successfully applied");
        }
    }

    private PaymentResponse toResponse(
            PaymentEventProjection event,
            long orderId,
            OrderStatus orderStatus,
            EntitlementGrantProjection grant
    ) {
        return new PaymentResponse(
                event.id(),
                event.provider(),
                event.providerEventId(),
                event.providerTransactionId(),
                event.processingStatus(),
                orderId,
                orderStatus,
                event.amountCents(),
                event.currency(),
                grant == null ? null : grant.id(),
                grant == null ? null : grant.status()
        );
    }
}
