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

    // readOnly 表达只读意图；是否可公开由 Mapper 的商品／修订／店铺状态条件共同决定。
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

        // 转义
        // #{searchPattern} 负责参数绑定；以下转义另用于让 LIKE 的 %、_ 按字面搜索。
        // 必须先转义 !，否则后面新加的转义前缀也会被再次处理。
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
