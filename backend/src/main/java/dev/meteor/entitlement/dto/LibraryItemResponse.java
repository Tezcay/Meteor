package dev.meteor.entitlement.dto;

import java.time.Instant;

public record LibraryItemResponse(
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
