package dev.meteor.product.service;

/** 有创作者角色但没有有效店铺时，不能继续创建商品；不会替用户悄悄开户。 */
public class CreatorShopUnavailableException extends RuntimeException {
    public CreatorShopUnavailableException() {
        super("An active creator shop is required");
    }
}
