package dev.meteor.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.product.dto.ProductDetailResponse;
import dev.meteor.product.dto.ProductListItemResponse;
import dev.meteor.product.mapper.ProductMapper;
import dev.meteor.product.mapper.projection.PackageOptionProjection;
import dev.meteor.product.mapper.projection.ProductSummaryProjection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductQueryServiceTest {

    private final StubProductMapper productMapper = new StubProductMapper();
    private final ProductQueryService productQueryService = new ProductQueryService(productMapper);

    @Test
    void returnsPublishedProductWithSalePackages() {
        productMapper.product = Optional.of(new ProductSummaryProjection(
                1001L,
                "Northstar Design",
                "求职材料模板包",
                "模板说明"
        ));
        productMapper.packages = List.of(
                new PackageOptionProjection(2001L, "Basic", 1900L, "CNY"),
                new PackageOptionProjection(2002L, "Pro", 3900L, "CNY")
        );

        ProductDetailResponse result = productQueryService.getPublishedProduct(1001L);

        assertThat(result.id()).isEqualTo(1001L);
        assertThat(result.packages()).extracting(ProductDetailResponse.PackageOptionResponse::name)
                .containsExactly("Basic", "Pro");
    }

    @Test
    void rejectsNonPositiveProductIdBeforeQueryingDatabase() {
        assertThatThrownBy(() -> productQueryService.getPublishedProduct(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productId must be positive");
    }

    @Test
    void reportsMissingPublishedProduct() {
        assertThatThrownBy(() -> productQueryService.getPublishedProduct(9999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void listsPublishedProductsWithEscapedLiteralSearch() {
        productMapper.list = List.of(new ProductSummaryProjection(
                1001L, "Northstar Design", "100% 求职模板", "模板说明"
        ));

        List<ProductListItemResponse> result = productQueryService.listPublishedProducts(" 100%_! ", "templates");

        assertThat(productMapper.lastSearchPattern).isEqualTo("%100!%!_!!%");
        assertThat(productMapper.lastCategorySlug).isEqualTo("templates");
        assertThat(result).extracting(ProductListItemResponse::title)
                .containsExactly("100% 求职模板");
    }

    @Test
    void listsAllPublishedProductsWithoutQuery() {
        productQueryService.listPublishedProducts(null, null);

        assertThat(productMapper.lastSearchPattern).isEqualTo("%%");
        assertThat(productMapper.lastCategorySlug).isNull();
    }

    @Test
    void rejectsOversizedSearchBeforeQueryingDatabase() {
        assertThatThrownBy(() -> productQueryService.listPublishedProducts("x".repeat(101), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
        assertThat(productMapper.lastSearchPattern).isNull();
    }

    @Test
    void rejectsInvalidCategorySlugBeforeQueryingDatabase() {
        assertThatThrownBy(() -> productQueryService.listPublishedProducts("", "设计素材"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
        assertThat(productMapper.lastSearchPattern).isNull();
    }

    private static final class StubProductMapper implements ProductMapper {
        private Optional<ProductSummaryProjection> product = Optional.empty();
        private List<PackageOptionProjection> packages = List.of();
        private List<ProductSummaryProjection> list = List.of();
        private String lastSearchPattern;
        private String lastCategorySlug;

        @Override
        public Optional<ProductSummaryProjection> findPublishedProduct(long productId) {
            return product;
        }

        @Override
        public List<PackageOptionProjection> findSalePackages(long productId) {
            return packages;
        }

        @Override
        public List<ProductSummaryProjection> findPublishedProducts(String searchPattern, String categorySlug) {
            lastSearchPattern = searchPattern;
            lastCategorySlug = categorySlug;
            return list;
        }
    }
}
