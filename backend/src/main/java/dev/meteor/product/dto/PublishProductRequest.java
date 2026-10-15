package dev.meteor.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 最小发布只配置一个付费套餐与一份已有素材；用户、币种和发布状态由服务端决定。 */
public record PublishProductRequest(
        @NotBlank @Size(max = 80) String packageName,
        @Min(1) @Max(100_000_000) long priceCents,
        @Positive long fileObjectId
) {
    public PublishProductRequest {
        packageName = packageName == null ? null : packageName.strip();
    }
}
