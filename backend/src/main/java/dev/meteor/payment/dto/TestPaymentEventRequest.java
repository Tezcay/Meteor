package dev.meteor.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record TestPaymentEventRequest(
        @NotBlank @Size(max = 100) String providerEventId,
        @NotBlank @Size(max = 100) String providerTransactionId,
        @Positive long amountCents,
        @NotBlank @Size(min = 3, max = 3) String currency
) {

    public TestPaymentEventRequest {
        providerEventId = providerEventId == null ? null : providerEventId.strip();
        providerTransactionId = providerTransactionId == null ? null : providerTransactionId.strip();
        currency = currency == null ? null : currency.strip().toUpperCase(Locale.ROOT);
    }
}
