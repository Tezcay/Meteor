package dev.meteor.payment.model;

public enum PaymentEventProcessingStatus {
    APPLIED,
    IGNORED,
    REJECTED;

    public boolean wasApplied() {
        return this == APPLIED;
    }
}
