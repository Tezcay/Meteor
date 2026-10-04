package dev.meteor.order.service;

public class OrderIdempotencyConflictException extends RuntimeException {

    public OrderIdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key has already been used for a different order: " + idempotencyKey);
    }
}
