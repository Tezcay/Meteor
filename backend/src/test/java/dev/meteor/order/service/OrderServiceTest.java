package dev.meteor.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.order.dto.CreateOrderRequest;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class OrderServiceTest {

    private final StubProductMapper productMapper = new StubProductMapper();
    private final StubOrderMapper orderMapper = new StubOrderMapper();
    private final OrderService orderService = new OrderService(productMapper, orderMapper);

    @Test
    void pricesOrderFromCurrentSalePackageAndStoresImmutableSnapshot() {
        productMapper.product = Optional.of(new ProductSummaryProjection(
                1001L, "Northstar Design", "求职材料模板包", "模板说明"
        ));
        productMapper.packageOption = Optional.of(new PackageOptionProjection(2002L, "Pro", 3900L, "CNY"));

        OrderResponse result = orderService.createOrder(
                2L,
                new CreateOrderRequest(1001L, 2002L, " checkout-001 ")
        );

        assertThat(result.id()).isEqualTo(7001L);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.totalAmountCents()).isEqualTo(3900L);
        assertThat(result.idempotencyKey()).isEqualTo("checkout-001");
        assertThat(orderMapper.orderItem.productTitle()).isEqualTo("求职材料模板包");
        assertThat(orderMapper.orderItem.shopName()).isEqualTo("Northstar Design");
        assertThat(orderMapper.orderItem.packageName()).isEqualTo("Pro");
        assertThat(orderMapper.orderItem.accessPolicy()).isEqualTo("ALL_RELEASES");
        assertThat(orderMapper.orderItem.unitPriceCents()).isEqualTo(3900L);
    }

    @Test
    void returnsPreviousOrderForSameIdempotencyKeyWithoutWritingAgain() {
        orderMapper.existing = Optional.of(snapshot(7001L, "checkout-001"));

        OrderResponse result = orderService.createOrder(
                2L,
                new CreateOrderRequest(1001L, 2001L, "checkout-001")
        );

        assertThat(result.id()).isEqualTo(7001L);
        assertThat(orderMapper.insertOrderCalls).isZero();
        assertThat(orderMapper.insertItemCalls).isZero();
    }

    @Test
    void rejectsReuseOfIdempotencyKeyForDifferentPackage() {
        orderMapper.existing = Optional.of(snapshot(7001L, "checkout-001"));

        assertThatThrownBy(() -> orderService.createOrder(
                2L,
                new CreateOrderRequest(1001L, 2002L, "checkout-001")
        )).isInstanceOf(OrderIdempotencyConflictException.class);
    }

    @Test
    void rejectsPackageThatIsNotCurrentlyForSale() {
        productMapper.product = Optional.of(new ProductSummaryProjection(
                1001L, "Northstar Design", "求职材料模板包", "模板说明"
        ));

        assertThatThrownBy(() -> orderService.createOrder(
                2L,
                new CreateOrderRequest(1001L, 9999L, "checkout-002")
        )).isInstanceOf(OrderPackageNotFoundException.class);
        assertThat(orderMapper.insertOrderCalls).isZero();
    }

    @Test
    void returnsConcurrentlyCreatedOrderWhenUniqueIdempotencyKeyWinsTheRace() {
        productMapper.product = Optional.of(new ProductSummaryProjection(
                1001L, "Northstar Design", "求职材料模板包", "模板说明"
        ));
        productMapper.packageOption = Optional.of(new PackageOptionProjection(2001L, "Basic", 1900L, "CNY"));
        orderMapper.throwDuplicateOnInsert = true;
        orderMapper.existingAfterDuplicate = Optional.of(snapshot(7002L, "checkout-race"));

        OrderResponse result = orderService.createOrder(
                2L,
                new CreateOrderRequest(1001L, 2001L, "checkout-race")
        );

        assertThat(result.id()).isEqualTo(7002L);
        assertThat(orderMapper.insertItemCalls).isZero();
    }

    @Test
    void returnsOnlyUserScopedOrderDetailsWithEntitlementState() {
        OrderDetailProjection detail = detail(7001L);
        orderMapper.details = List.of(detail);
        orderMapper.detail = Optional.of(detail);

        assertThat(orderService.getOrders(2L))
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.id()).isEqualTo(7001L);
                    assertThat(order.entitlementGrantId()).isEqualTo(8101L);
                    assertThat(order.entitlementStatus()).isEqualTo("ACTIVE");
                });
        assertThat(orderService.getOrder(2L, 7001L).shopName()).isEqualTo("Northstar Design");
    }

    @Test
    void hidesMissingOrForeignOrderBehindNotFound() {
        assertThatThrownBy(() -> orderService.getOrder(2L, 7999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private static OrderSnapshotProjection snapshot(long id, String idempotencyKey) {
        return new OrderSnapshotProjection(
                id, 2L, OrderStatus.PENDING_PAYMENT, 1900L, "CNY", idempotencyKey,
                1001L, 2001L, "求职材料模板包", "Northstar Design", "Basic", "ALL_RELEASES"
        );
    }

    private static OrderDetailProjection detail(long id) {
        Instant createdAt = Instant.parse("2026-10-02T12:00:00Z");
        return new OrderDetailProjection(
                id, OrderStatus.PAID, 1900L, "CNY", createdAt,
                1001L, 2001L, "求职材料模板包", "Northstar Design", "Basic", "ALL_RELEASES",
                8101L, "ACTIVE", createdAt.plusSeconds(3)
        );
    }

    private static final class StubProductMapper implements ProductMapper {
        private Optional<ProductSummaryProjection> product = Optional.empty();
        private Optional<PackageOptionProjection> packageOption = Optional.empty();

        @Override
        public Optional<ProductSummaryProjection> findPublishedProduct(long productId) {
            return product;
        }

        @Override
        public List<PackageOptionProjection> findSalePackages(long productId) {
            return packageOption.stream().toList();
        }

        @Override
        public Optional<PackageOptionProjection> findSalePackage(long productId, long packageId) {
            return packageOption.filter(option -> option.id() == packageId);
        }

        @Override
        public List<ProductSummaryProjection> findPublishedProducts(String searchPattern, String categorySlug) {
            return List.of();
        }
    }

    private static final class StubOrderMapper implements OrderMapper {
        private Optional<OrderSnapshotProjection> existing = Optional.empty();
        private Optional<OrderSnapshotProjection> existingAfterDuplicate = Optional.empty();
        private OrderItemInsert orderItem;
        private int insertOrderCalls;
        private int insertItemCalls;
        private boolean throwDuplicateOnInsert;
        private List<OrderDetailProjection> details = List.of();
        private Optional<OrderDetailProjection> detail = Optional.empty();

        @Override
        public Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey) {
            return existing;
        }

        @Override
        public Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKeyForUpdate(
                long userId,
                String idempotencyKey
        ) {
            return existingAfterDuplicate;
        }

        @Override
        public int insertOrder(OrderInsert order) {
            insertOrderCalls++;
            if (throwDuplicateOnInsert) {
                throw new DuplicateKeyException("simulated concurrent idempotency collision");
            }
            order.setId(7001L);
            return 1;
        }

        @Override
        public int insertOrderItem(OrderItemInsert item) {
            insertItemCalls++;
            orderItem = item;
            return 1;
        }

        @Override
        public Optional<OrderSnapshotProjection> findByIdAndUserId(long orderId, long userId) {
            return existing;
        }

        @Override
        public Optional<OrderSnapshotProjection> findByIdAndUserIdForUpdate(long orderId, long userId) {
            return existing;
        }

        @Override
        public int markPaid(long orderId, long userId) {
            return 1;
        }

        @Override
        public List<OrderDetailProjection> findAllByUserId(long userId) {
            return details;
        }

        @Override
        public Optional<OrderDetailProjection> findDetailByIdAndUserId(long orderId, long userId) {
            return detail;
        }
    }
}
