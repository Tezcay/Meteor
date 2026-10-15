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

/** 下单只信任服务端商品和套餐；同次购买重试返回成交快照，订单头与订单项一起提交。 */
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
        // 幂等键限定在当前用户内。先重放历史快照，避免重试时按新价格重新成交。
        OrderSnapshotProjection existing = orderMapper
                .findByUserIdAndIdempotencyKey(userId, request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            if (existing.productId() != request.productId() || existing.packageId() != request.packageId()) {
                throw new OrderIdempotencyConflictException(request.idempotencyKey());
            }
            return toResponse(existing);
        }

        // 客户端只给商品和套餐 ID；可售状态、价格与币种都从数据库重新读取。
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
            // 两个请求可能同时查不到旧订单，最终由唯一约束裁决。
            // 用 FOR UPDATE 当前读取得胜出事务的订单，避免重复读快照仍看不到它。
            OrderSnapshotProjection concurrentlyCreated = orderMapper
                    .findByUserIdAndIdempotencyKeyForUpdate(userId, request.idempotencyKey())
                    .orElseThrow(() -> duplicateKeyException);
            if (concurrentlyCreated.productId() != request.productId()
                    || concurrentlyCreated.packageId() != request.packageId()) {
                throw new OrderIdempotencyConflictException(request.idempotencyKey());
            }
            return toResponse(concurrentlyCreated);
        }
        // 保存当时的标题、套餐和价格；以后商品变更不能改写这笔订单的购买事实。
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
        // 订单 ID 不是访问凭证。把当前用户放入查询，不存在和越权都走相同的 404。
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
