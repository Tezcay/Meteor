package dev.meteor.payment.mapper.projection;

public record PaymentEventProjection(
        long id,
        long paymentAttemptId,
        String provider,
        String providerEventId,
        String providerTransactionId,
        String processingStatus,
        long amountCents,
        String currency
) {
}
