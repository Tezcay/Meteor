package dev.meteor.payment.mapper.projection;

import dev.meteor.payment.model.PaymentAttemptStatus;

public record PaymentAttemptProjection(
        long id,
        long orderId,
        String provider,
        String providerTransactionId,
        PaymentAttemptStatus status,
        long amountCents,
        String currency
) {
}
