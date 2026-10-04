package dev.meteor.payment.dto;

import dev.meteor.order.model.OrderStatus;

public record PaymentResponse(
        long eventId,
        String provider,
        String providerEventId,
        String providerTransactionId,
        String processingStatus,
        long orderId,
        OrderStatus orderStatus,
        long amountCents,
        String currency,
        Long entitlementGrantId,
        String entitlementStatus
) {
}
