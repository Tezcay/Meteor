package dev.meteor.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @Positive long productId,
        @Positive long packageId,
        @NotBlank @Size(max = 100) String idempotencyKey
) {

    public CreateOrderRequest {
        idempotencyKey = idempotencyKey == null ? null : idempotencyKey.strip();
    }
}
