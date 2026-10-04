package dev.meteor.entitlement.mapper.projection;

import java.time.Instant;

public record LibraryItemProjection(
        long grantId,
        long orderId,
        long orderItemId,
        long productId,
        long packageId,
        String productTitle,
        String shopName,
        String packageName,
        String accessPolicy,
        String grantStatus,
        Instant grantedAt
) {
}
