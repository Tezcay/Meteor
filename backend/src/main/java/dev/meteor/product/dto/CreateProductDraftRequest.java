package dev.meteor.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建草稿只接收展示信息。用户、店铺和状态由后端决定，不能由客户端指定。 */
public record CreateProductDraftRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 500) String summary
) {
    // 先规范化再由 @Valid 校验；null 保留给 @NotBlank 处理，不在这里调用 trim。
    public CreateProductDraftRequest {
        title = title == null ? null : title.strip();
        summary = summary == null ? null : summary.strip();
    }
}
