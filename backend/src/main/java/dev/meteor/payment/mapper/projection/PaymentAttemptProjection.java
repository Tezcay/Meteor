package dev.meteor.payment.mapper.projection;

public record PaymentAttemptProjection(
        long id,
        long orderId,
        String provider,
        String providerTransactionId,
        String status,
        long amountCents,
        String currency
) {
}
