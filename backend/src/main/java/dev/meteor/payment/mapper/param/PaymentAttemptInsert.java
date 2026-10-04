package dev.meteor.payment.mapper.param;

import dev.meteor.payment.model.PaymentAttemptStatus;

public final class PaymentAttemptInsert {

    private long id;
    private final long orderId;
    private final String provider;
    private final String providerTransactionId;
    private final PaymentAttemptStatus status;
    private final long amountCents;
    private final String currency;

    public PaymentAttemptInsert(
            long orderId,
            String provider,
            String providerTransactionId,
            PaymentAttemptStatus status,
            long amountCents,
            String currency
    ) {
        this.orderId = orderId;
        this.provider = provider;
        this.providerTransactionId = providerTransactionId;
        this.status = status;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public PaymentAttemptStatus getStatus() {
        return status;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }
}
