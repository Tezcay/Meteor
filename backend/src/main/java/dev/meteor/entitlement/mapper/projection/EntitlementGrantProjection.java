package dev.meteor.entitlement.mapper.projection;

import java.time.Instant;

public record EntitlementGrantProjection(
        long id,
        long userId,
        long orderItemId,
        long productId,
        long packageId,
        String accessPolicy,
        String status,
        Instant grantedAt,
        Instant revokedAt
) {
}
