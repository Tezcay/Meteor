package dev.meteor.payment.model;

public enum PaymentAttemptStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    RECONCILING;

    public boolean acceptsSuccess() {
        return this == PENDING;
    }

    public boolean hasSucceeded() {
        return this == SUCCEEDED;
    }
}
