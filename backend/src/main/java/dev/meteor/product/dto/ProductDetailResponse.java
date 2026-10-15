package dev.meteor.product.dto;

import java.util.List;

public record ProductDetailResponse(
        long id,
        String shopName,
        String title,
        String summary,
        List<PackageOptionResponse> packages  // 可售套餐列表
) {
    public ProductDetailResponse {
        packages = List.copyOf(packages);
    }

    public record PackageOptionResponse(
            long id,
            String name,
            long priceCents,
            String currency
    ) {
    }
}
