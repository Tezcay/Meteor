package dev.meteor.product.mapper.projection;

public record PackageOptionProjection(
        long id,
        String name,
        long priceCents,
        String currency
) {
}
