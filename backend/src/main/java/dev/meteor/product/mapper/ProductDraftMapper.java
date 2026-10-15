package dev.meteor.product.mapper;

import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.mapper.param.ProductDraftInsert;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** SQL 在 ProductDraftMapper.xml 中，接口只声明输入和返回值。 */
public interface ProductDraftMapper {
    // Long 可以返回 null（没有有效店铺），long 则无法表示“没查到”。
    Long findActiveShopForUpdate(@Param("userId") long userId);

    int insertProduct(ProductDraftInsert draft);
    int insertRevision(ProductDraftInsert draft);

    ProductDraftResponse findOwnedDraft(
            @Param("userId") long userId,
            @Param("productId") long productId,
            @Param("revisionId") long revisionId
    );

    List<ProductDraftResponse> findOwnDrafts(@Param("userId") long userId);
}
