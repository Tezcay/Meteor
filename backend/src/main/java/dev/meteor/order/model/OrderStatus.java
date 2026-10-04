package dev.meteor.order.model;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CLOSED,
    REFUNDED;

    public boolean acceptsPayment() {
        return this == PENDING_PAYMENT;
    }

    public boolean hasPaid() {
        return this == PAID;
    }
}
