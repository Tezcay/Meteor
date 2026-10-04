package dev.meteor.order.service;

public class OrderPackageNotFoundException extends RuntimeException {

    public OrderPackageNotFoundException(long productId, long packageId) {
        super("Sale package " + packageId + " was not found for product " + productId);
    }
}
