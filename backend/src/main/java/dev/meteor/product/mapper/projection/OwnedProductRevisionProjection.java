package dev.meteor.product.mapper.projection;

public record OwnedProductRevisionProjection(
        long productId, long revisionId, Long publishedRevisionId,
        String revisionStatus, String saleStatus, String summary
) {}
