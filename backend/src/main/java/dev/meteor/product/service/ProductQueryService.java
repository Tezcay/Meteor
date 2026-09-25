package dev.meteor.product.service;

import dev.meteor.product.dto.ProductDetailResponse;
import dev.meteor.product.dto.ProductListItemResponse;
import dev.meteor.product.mapper.ProductMapper;
import dev.meteor.product.mapper.projection.PackageOptionProjection;
import dev.meteor.product.mapper.projection.ProductSummaryProjection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductMapper productMapper;

    public ProductQueryService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublishedProduct(long productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }

        ProductSummaryProjection product = productMapper.findPublishedProduct(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        List<ProductDetailResponse.PackageOptionResponse> packages = productMapper.findSalePackages(productId).stream()
                .map(this::toPackageOptionResponse)
                .toList();

        return new ProductDetailResponse(
                product.id(),
                product.shopName(),
                product.title(),
                product.summary(),
                packages
        );
    }

    @Transactional(readOnly = true)
    public List<ProductListItemResponse> listPublishedProducts(String query, String category) {
        String normalized = query == null ? "" : query.strip();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("query must not exceed 100 characters");
        }

        String normalizedCategory = category == null || category.isBlank() ? null : category.strip();
        if (normalizedCategory != null && !normalizedCategory.matches("[a-z0-9-]{1,50}")) {
            throw new IllegalArgumentException("category must be a valid slug");
        }

        String searchPattern = "%" + normalized
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";

        return productMapper.findPublishedProducts(searchPattern, normalizedCategory).stream()
                .map(product -> new ProductListItemResponse(
                        product.id(),
                        product.shopName(),
                        product.title(),
                        product.summary()
                ))
                .toList();
    }

    private ProductDetailResponse.PackageOptionResponse toPackageOptionResponse(PackageOptionProjection projection) {
        return new ProductDetailResponse.PackageOptionResponse(
                projection.id(),
                projection.name(),
                projection.priceCents(),
                projection.currency()
        );
    }
}
