package dev.meteor.payment.mapper.param;

public final class PaymentEventInsert {

    private long id;
    private final long paymentAttemptId;
    private final String provider;
    private final String providerEventId;
    private final String providerTransactionId;
    private final String eventType;
    private final long amountCents;
    private final String currency;
    private final String processingStatus;

    public PaymentEventInsert(
            long paymentAttemptId,
            String provider,
            String providerEventId,
            String providerTransactionId,
            String eventType,
            long amountCents,
            String currency,
            String processingStatus
    ) {
        this.paymentAttemptId = paymentAttemptId;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.providerTransactionId = providerTransactionId;
        this.eventType = eventType;
        this.amountCents = amountCents;
        this.currency = currency;
        this.processingStatus = processingStatus;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPaymentAttemptId() {
        return paymentAttemptId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getEventType() {
        return eventType;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }
}
