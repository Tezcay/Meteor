package dev.meteor.order.mapper.projection;

import dev.meteor.order.model.OrderStatus;

public record OrderSnapshotProjection(
        long id,
        long userId,
        OrderStatus status,
        long totalAmountCents,
        String currency,
        String idempotencyKey,
        long productId,
        long packageId,
        String productTitle,
        String shopName,
        String packageName,
        String accessPolicy
) {
}
