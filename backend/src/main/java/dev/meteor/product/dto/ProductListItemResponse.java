package dev.meteor.product.dto;

public record ProductListItemResponse(
        long id,
        String shopName,
        String title,
        String summary
) {
}
