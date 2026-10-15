package dev.meteor.product.service;

/** 资源不存在与不属于当前用户使用同一响应，避免泄露他人草稿。 */
public class ProductDraftNotFoundException extends RuntimeException {
    public ProductDraftNotFoundException() { super("Product draft was not found"); }
}
