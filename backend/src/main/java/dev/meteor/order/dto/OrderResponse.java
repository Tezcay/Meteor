package dev.meteor.order.dto;

import dev.meteor.order.model.OrderStatus;

public record OrderResponse(
        long id,
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
