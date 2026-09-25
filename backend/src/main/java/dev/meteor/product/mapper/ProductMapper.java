package dev.meteor.product.mapper;

import dev.meteor.product.mapper.projection.PackageOptionProjection;
import dev.meteor.product.mapper.projection.ProductSummaryProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

public interface ProductMapper {

    Optional<ProductSummaryProjection> findPublishedProduct(@Param("productId") long productId);

    List<ProductSummaryProjection> findPublishedProducts(
            @Param("searchPattern") String searchPattern,
            @Param("categorySlug") String categorySlug
    );

    List<PackageOptionProjection> findSalePackages(@Param("productId") long productId);
}
