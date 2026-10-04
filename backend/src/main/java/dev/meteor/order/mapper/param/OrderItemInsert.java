package dev.meteor.order.mapper.param;

public record OrderItemInsert(
        long orderId,
        long productId,
        long packageId,
        String productTitle,
        String shopName,
        String packageName,
        String accessPolicy,
        long unitPriceCents,
        String currency,
        int quantity
) {
}
