package dev.meteor.product.dto;

import java.util.List;

public record ProductDetailResponse(
        long id,
        String shopName,
        String title,
        String summary,
        List<PackageOptionResponse> packages
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
