package dev.meteor.order.service;

import dev.meteor.order.dto.CreateOrderRequest;
import dev.meteor.order.dto.OrderDetailResponse;
import dev.meteor.order.dto.OrderResponse;
import dev.meteor.order.mapper.OrderMapper;
import dev.meteor.order.mapper.param.OrderInsert;
import dev.meteor.order.mapper.param.OrderItemInsert;
import dev.meteor.order.mapper.projection.OrderDetailProjection;
import dev.meteor.order.mapper.projection.OrderSnapshotProjection;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.product.mapper.ProductMapper;
import dev.meteor.product.mapper.projection.PackageOptionProjection;
import dev.meteor.product.mapper.projection.ProductSummaryProjection;
import dev.meteor.product.service.ProductNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrderService {

    private static final String ACCESS_POLICY = "ALL_RELEASES";

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;

    public OrderService(ProductMapper productMapper, OrderMapper orderMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse createOrder(long userId, CreateOrderRequest request) {
        OrderSnapshotProjection existing = orderMapper
                .findByUserIdAndIdempotencyKey(userId, request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            if (existing.productId() != request.productId() || existing.packageId() != request.packageId()) {
                throw new OrderIdempotencyConflictException(request.idempotencyKey());
            }
            return toResponse(existing);
        }

        ProductSummaryProjection product = productMapper.findPublishedProduct(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        PackageOptionProjection packageOption = productMapper
                .findSalePackage(request.productId(), request.packageId())
                .orElseThrow(() -> new OrderPackageNotFoundException(request.productId(), request.packageId()));

        OrderInsert order = new OrderInsert(
                userId,
                OrderStatus.PENDING_PAYMENT,
                packageOption.priceCents(),
                packageOption.currency(),
                request.idempotencyKey()
        );
        try {
            orderMapper.insertOrder(order);
        } catch (DuplicateKeyException duplicateKeyException) {
            OrderSnapshotProjection concurrentlyCreated = orderMapper
                    .findByUserIdAndIdempotencyKeyForUpdate(userId, request.idempotencyKey())
                    .orElseThrow(() -> duplicateKeyException);
            if (concurrentlyCreated.productId() != request.productId()
                    || concurrentlyCreated.packageId() != request.packageId()) {
                throw new OrderIdempotencyConflictException(request.idempotencyKey());
            }
            return toResponse(concurrentlyCreated);
        }
        orderMapper.insertOrderItem(new OrderItemInsert(
                order.getId(),
                product.id(),
                packageOption.id(),
                product.title(),
                product.shopName(),
                packageOption.name(),
                ACCESS_POLICY,
                packageOption.priceCents(),
                packageOption.currency(),
                1
        ));

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmountCents(),
                order.getCurrency(),
                order.getIdempotencyKey(),
                product.id(),
                packageOption.id(),
                product.title(),
                product.shopName(),
                packageOption.name(),
                ACCESS_POLICY
        );
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrders(long userId) {
        return orderMapper.findAllByUserId(userId).stream()
                .map(OrderService::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(long userId, long orderId) {
        return orderMapper.findDetailByIdAndUserId(orderId, userId)
                .map(OrderService::toDetailResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderResponse toResponse(OrderSnapshotProjection order) {
        return new OrderResponse(
                order.id(),
                order.status(),
                order.totalAmountCents(),
                order.currency(),
                order.idempotencyKey(),
                order.productId(),
                order.packageId(),
                order.productTitle(),
                order.shopName(),
                order.packageName(),
                order.accessPolicy()
        );
    }

    private static OrderDetailResponse toDetailResponse(OrderDetailProjection order) {
        return new OrderDetailResponse(
                order.id(),
                order.status(),
                order.totalAmountCents(),
                order.currency(),
                order.createdAt(),
                order.productId(),
                order.packageId(),
                order.productTitle(),
                order.shopName(),
                order.packageName(),
                order.accessPolicy(),
                order.entitlementGrantId(),
                order.entitlementStatus(),
                order.grantedAt()
        );
    }
}
