package dev.meteor.payment.dto;

import dev.meteor.order.model.OrderStatus;
import dev.meteor.payment.model.PaymentEventProcessingStatus;

public record PaymentResponse(
        long eventId,
        String provider,
        String providerEventId,
        String providerTransactionId,
        PaymentEventProcessingStatus processingStatus,
        long orderId,
        OrderStatus orderStatus,
        long amountCents,
        String currency,
        Long entitlementGrantId,
        String entitlementStatus
) {
}
