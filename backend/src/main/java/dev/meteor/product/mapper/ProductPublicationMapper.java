package dev.meteor.product.mapper;

import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.dto.ProductPublicationResponse;
import dev.meteor.product.mapper.param.ProductPublicationInsert;
import dev.meteor.product.mapper.projection.OwnedProductRevisionProjection;
import dev.meteor.product.mapper.projection.ReusableDeliveryFileProjection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProductPublicationMapper {
    OwnedProductRevisionProjection findOwnedRevisionForUpdate(
            @Param("userId") long userId, @Param("productId") long productId);
    List<ProductDraftResponse> findOwnProducts(@Param("userId") long userId);
    List<ReusableDeliveryFileProjection> findReusableFiles(@Param("userId") long userId);
    ReusableDeliveryFileProjection findReusableFileForUpdate(
            @Param("userId") long userId, @Param("fileObjectId") long fileObjectId);
    ProductPublicationResponse findPublication(
            @Param("userId") long userId, @Param("productId") long productId);
    int insertPackage(ProductPublicationInsert publication);
    int insertRelease(ProductPublicationInsert publication);
    int insertManifest(ProductPublicationInsert publication);
    int insertManifestItem(ProductPublicationInsert publication);
    int markRevisionPublished(ProductPublicationInsert publication);
    int markProductOnSale(ProductPublicationInsert publication);
}
