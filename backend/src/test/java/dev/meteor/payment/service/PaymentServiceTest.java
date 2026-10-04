package dev.meteor.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import dev.meteor.order.mapper.OrderMapper;
import dev.meteor.order.mapper.param.OrderInsert;
import dev.meteor.order.mapper.param.OrderItemInsert;
import dev.meteor.order.mapper.projection.OrderDetailProjection;
import dev.meteor.order.mapper.projection.OrderSnapshotProjection;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.payment.dto.PaymentResponse;
import dev.meteor.payment.dto.TestPaymentEventRequest;
import dev.meteor.payment.mapper.PaymentMapper;
import dev.meteor.payment.mapper.param.PaymentAttemptInsert;
import dev.meteor.payment.mapper.param.PaymentEventInsert;
import dev.meteor.payment.mapper.projection.PaymentAttemptProjection;
import dev.meteor.payment.mapper.projection.PaymentEventProjection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private final StubOrderMapper orderMapper = new StubOrderMapper();
    private final StubPaymentMapper paymentMapper = new StubPaymentMapper();
    private final StubEntitlementMapper entitlementMapper = new StubEntitlementMapper();
    private final PaymentService paymentService = new PaymentService(orderMapper, paymentMapper, entitlementMapper);

    @Test
    void appliesMatchingTestPaymentAndMovesOrderToPaid() {
        orderMapper.order = Optional.of(order(OrderStatus.PENDING_PAYMENT));

        PaymentResponse result = paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-001", "txn-001", 1900L, "cny")
        );

        assertThat(result.eventId()).isEqualTo(4001L);
        assertThat(result.processingStatus()).isEqualTo("APPLIED");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentMapper.insertAttemptCalls).isEqualTo(1);
        assertThat(paymentMapper.insertEventCalls).isEqualTo(1);
        assertThat(orderMapper.markPaidCalls).isEqualTo(1);
        assertThat(result.entitlementGrantId()).isEqualTo(5001L);
        assertThat(result.entitlementStatus()).isEqualTo("ACTIVE");
        assertThat(entitlementMapper.ensureCalls).isEqualTo(1);
    }

    @Test
    void replaysSameProviderEventWithoutApplyingItTwice() {
        orderMapper.order = Optional.of(order(OrderStatus.PAID));
        paymentMapper.eventByEventId = Optional.of(event("evt-001", "txn-001"));
        paymentMapper.attemptByTransaction = Optional.of(new PaymentAttemptProjection(
                3001L, 7001L, PaymentService.PROVIDER, "txn-001", "SUCCEEDED", 1900L, "CNY"
        ));

        PaymentResponse result = paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-001", "txn-001", 1900L, "CNY")
        );

        assertThat(result.eventId()).isEqualTo(4001L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentMapper.insertAttemptCalls).isZero();
        assertThat(paymentMapper.insertEventCalls).isZero();
        assertThat(orderMapper.markPaidCalls).isZero();
        assertThat(entitlementMapper.ensureCalls).isEqualTo(1);
    }

    @Test
    void rejectsReusingEventIdForAnotherTransaction() {
        orderMapper.order = Optional.of(order(OrderStatus.PAID));
        paymentMapper.eventByEventId = Optional.of(event("evt-001", "txn-original"));
        paymentMapper.attemptByTransaction = Optional.of(new PaymentAttemptProjection(
                3001L, 7001L, PaymentService.PROVIDER, "txn-original", "SUCCEEDED", 1900L, "CNY"
        ));

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-001", "txn-other", 1900L, "CNY")
        )).isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("reused");
    }

    @Test
    void rejectedEventCannotSelfHealAnEntitlement() {
        orderMapper.order = Optional.of(order(OrderStatus.PAID));
        paymentMapper.eventByEventId = Optional.of(new PaymentEventProjection(
                4001L,
                3001L,
                PaymentService.PROVIDER,
                "evt-rejected",
                "txn-rejected",
                "REJECTED",
                1900L,
                "CNY"
        ));
        paymentMapper.attemptByTransaction = Optional.of(new PaymentAttemptProjection(
                3001L, 7001L, PaymentService.PROVIDER, "txn-rejected", "FAILED", 1900L, "CNY"
        ));

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-rejected", "txn-rejected", 1900L, "CNY")
        )).isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("not successfully applied");
        assertThat(entitlementMapper.ensureCalls).isZero();
    }

    @Test
    void replaysSameTransactionEvenWhenProviderUsesAnotherEventId() {
        orderMapper.order = Optional.of(order(OrderStatus.PAID));
        paymentMapper.eventByTransactionId = Optional.of(event("evt-original", "txn-001"));
        paymentMapper.attemptByTransaction = Optional.of(new PaymentAttemptProjection(
                3001L, 7001L, PaymentService.PROVIDER, "txn-001", "SUCCEEDED", 1900L, "CNY"
        ));

        PaymentResponse result = paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-retry", "txn-001", 1900L, "CNY")
        );

        assertThat(result.providerEventId()).isEqualTo("evt-original");
        assertThat(paymentMapper.insertEventCalls).isZero();
    }

    @Test
    void rejectsWrongAmountBeforeWritingPaymentFacts() {
        orderMapper.order = Optional.of(order(OrderStatus.PENDING_PAYMENT));

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-002", "txn-002", 3900L, "CNY")
        )).isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("amount or currency");
        assertThat(paymentMapper.insertAttemptCalls).isZero();
        assertThat(paymentMapper.insertEventCalls).isZero();
    }

    @Test
    void rejectsDifferentTransactionForAnExistingAttempt() {
        orderMapper.order = Optional.of(order(OrderStatus.PENDING_PAYMENT));
        paymentMapper.attemptByOrder = Optional.of(new PaymentAttemptProjection(
                3001L, 7001L, PaymentService.PROVIDER, "txn-existing", "PENDING", 1900L, "CNY"
        ));

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-003", "txn-other", 1900L, "CNY")
        )).isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("different payment transaction");
        assertThat(paymentMapper.insertEventCalls).isZero();
    }

    @Test
    void rejectsPaymentForAlreadyPaidOrderWhenEventIsNew() {
        orderMapper.order = Optional.of(order(OrderStatus.PAID));

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-004", "txn-004", 1900L, "CNY")
        )).isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("already PAID");
        assertThat(paymentMapper.insertAttemptCalls).isZero();
    }

    @Test
    void failsThePaymentTransactionWhenPaidOrderHasNoGrant() {
        orderMapper.order = Optional.of(order(OrderStatus.PENDING_PAYMENT));
        entitlementMapper.grant = Optional.empty();

        assertThatThrownBy(() -> paymentService.applyTestPayment(
                2L,
                7001L,
                new TestPaymentEventRequest("evt-no-grant", "txn-no-grant", 1900L, "CNY")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entitlement grant");
        assertThat(entitlementMapper.ensureCalls).isEqualTo(1);
    }

    private static OrderSnapshotProjection order(OrderStatus status) {
        return new OrderSnapshotProjection(
                7001L,
                2L,
                status,
                1900L,
                "CNY",
                "checkout-001",
                1001L,
                2001L,
                "求职材料模板包",
                "Northstar Design",
                "Basic",
                "ALL_RELEASES"
        );
    }

    private static PaymentEventProjection event(String eventId, String transactionId) {
        return new PaymentEventProjection(
                4001L,
                3001L,
                PaymentService.PROVIDER,
                eventId,
                transactionId,
                "APPLIED",
                1900L,
                "CNY"
        );
    }

    private static final class StubOrderMapper implements OrderMapper {
        private Optional<OrderSnapshotProjection> order = Optional.empty();
        private int markPaidCalls;

        @Override
        public Optional<OrderSnapshotProjection> findByIdAndUserId(long orderId, long userId) {
            return order;
        }

        @Override
        public Optional<OrderSnapshotProjection> findByIdAndUserIdForUpdate(long orderId, long userId) {
            return order;
        }

        @Override
        public int markPaid(long orderId, long userId) {
            markPaidCalls++;
            return 1;
        }

        @Override
        public Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKeyForUpdate(
                long userId,
                String idempotencyKey
        ) {
            return Optional.empty();
        }

        @Override
        public int insertOrder(OrderInsert order) {
            return 1;
        }

        @Override
        public int insertOrderItem(OrderItemInsert item) {
            return 1;
        }

        @Override
        public List<OrderDetailProjection> findAllByUserId(long userId) {
            return List.of();
        }

        @Override
        public Optional<OrderDetailProjection> findDetailByIdAndUserId(long orderId, long userId) {
            return Optional.empty();
        }
    }

    private static final class StubEntitlementMapper implements EntitlementMapper {
        private int ensureCalls;
        private Optional<EntitlementGrantProjection> grant = Optional.of(new EntitlementGrantProjection(
                5001L,
                2L,
                8001L,
                1001L,
                2001L,
                "ALL_RELEASES",
                "ACTIVE",
                Instant.parse("2026-10-03T00:00:00Z"),
                null
        ));

        @Override
        public int ensureGrantForPaidOrder(long orderId, long userId) {
            ensureCalls++;
            return 1;
        }

        @Override
        public Optional<EntitlementGrantProjection> findByOrderIdAndUserId(long orderId, long userId) {
            return grant;
        }

        @Override
        public Optional<EntitlementGrantProjection> findActiveByIdAndUserId(long grantId, long userId) {
            return Optional.empty();
        }

        @Override
        public List<LibraryItemProjection> findActiveLibraryByUserId(long userId) {
            return List.of();
        }
    }

    private static final class StubPaymentMapper implements PaymentMapper {
        private Optional<PaymentAttemptProjection> attemptByOrder = Optional.empty();
        private Optional<PaymentAttemptProjection> attemptByTransaction = Optional.empty();
        private Optional<PaymentEventProjection> eventByEventId = Optional.empty();
        private Optional<PaymentEventProjection> eventByTransactionId = Optional.empty();
        private int insertAttemptCalls;
        private int insertEventCalls;

        @Override
        public Optional<PaymentAttemptProjection> findAttemptByOrderId(long orderId) {
            return attemptByOrder;
        }

        @Override
        public Optional<PaymentAttemptProjection> findAttemptByProviderTransactionId(
                String provider,
                String providerTransactionId
        ) {
            return attemptByTransaction;
        }

        @Override
        public Optional<PaymentEventProjection> findEventByProviderEventId(
                String provider,
                String providerEventId
        ) {
            return eventByEventId;
        }

        @Override
        public Optional<PaymentEventProjection> findEventByProviderTransactionId(
                String provider,
                String providerTransactionId
        ) {
            return eventByTransactionId;
        }

        @Override
        public int insertAttempt(PaymentAttemptInsert attempt) {
            insertAttemptCalls++;
            attempt.setId(3001L);
            return 1;
        }

        @Override
        public int insertEvent(PaymentEventInsert event) {
            insertEventCalls++;
            event.setId(4001L);
            return 1;
        }

        @Override
        public int markAttemptSucceeded(long attemptId) {
            return 1;
        }
    }
}
