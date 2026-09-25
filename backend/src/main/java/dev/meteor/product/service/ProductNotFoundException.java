package dev.meteor.product.service;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long productId) {
        super("Published product " + productId + " was not found");
    }
}
