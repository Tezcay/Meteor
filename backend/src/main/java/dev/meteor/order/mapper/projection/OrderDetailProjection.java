package dev.meteor.order.mapper.projection;

import dev.meteor.order.model.OrderStatus;
import java.time.Instant;

public record OrderDetailProjection(
        long id,
        OrderStatus status,
        long totalAmountCents,
        String currency,
        Instant createdAt,
        long productId,
        long packageId,
        String productTitle,
        String shopName,
        String packageName,
        String accessPolicy,
        Long entitlementGrantId,
        String entitlementStatus,
        Instant grantedAt
) {
}
