package dev.meteor.payment.service;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(long orderId) {
        super("Order " + orderId + " was not found for the current user");
    }
}
