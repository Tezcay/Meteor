package dev.meteor.product.dto;

import java.time.Instant;

/** 草稿接口的响应，同时用于这组固定查询列的 MyBatis 结果映射，不是完整数据库实体。 */
public record ProductDraftResponse(
        long productId,
        long revisionId,
        String title,
        String summary,
        String revisionStatus,
        String saleStatus,
        Instant createdAt
) {
}
