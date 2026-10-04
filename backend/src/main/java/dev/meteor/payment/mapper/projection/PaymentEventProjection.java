package dev.meteor.payment.mapper.projection;

import dev.meteor.payment.model.PaymentEventProcessingStatus;
import dev.meteor.payment.model.PaymentEventType;

public record PaymentEventProjection(
        long id,
        long paymentAttemptId,
        String provider,
        String providerEventId,
        String providerTransactionId,
        PaymentEventType eventType,
        PaymentEventProcessingStatus processingStatus,
        long amountCents,
        String currency
) {
}
