package dev.meteor.product.dto;

/** 发布结果来自数据库；重放时也返回同一组套餐、版本与清单 ID。 */
public record ProductPublicationResponse(
        long productId, long revisionId, long packageId, long releaseId,
        long manifestId, long manifestItemId,
        String packageName, long priceCents, String currency, long fileObjectId
) {}
