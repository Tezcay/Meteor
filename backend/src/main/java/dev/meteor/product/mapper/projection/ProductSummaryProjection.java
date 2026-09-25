package dev.meteor.product.mapper.projection;

public record ProductSummaryProjection(
        long id,
        String shopName,
        String title,
        String summary
) {
}
