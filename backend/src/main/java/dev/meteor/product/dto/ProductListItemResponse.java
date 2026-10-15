package dev.meteor.product.dto;

public record ProductListItemResponse(
        long id,  // 商品id
        String shopName,  // 店铺名
        String title,  // 标题
        String summary // 简介
) {
}
