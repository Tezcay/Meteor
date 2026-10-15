package dev.meteor.product.service;

import dev.meteor.product.dto.CreateProductDraftRequest;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.mapper.ProductDraftMapper;
import dev.meteor.product.mapper.param.ProductDraftInsert;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 草稿业务：只准备展示信息，不创建套餐、发布版本或购买权益。 */
@Service
public class ProductDraftService {
    private final ProductDraftMapper mapper;

    public ProductDraftService(ProductDraftMapper mapper) {
        this.mapper = mapper;
    }

    // 两张表必须一起成功；第二次插入或后续查询失败时，第一次插入也会回滚。
    @Transactional
    public ProductDraftResponse createDraft(long userId, CreateProductDraftRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.title().length() > 160 || request.summary() == null
                || request.summary().isBlank() || request.summary().length() > 500) {
            throw new IllegalArgumentException("A title (1-160) and summary (1-500) are required");
        }
        // 从当前 Session 用户找店铺并锁行，不接收客户端 shopId；创建期间店铺状态不能被并发修改。
        Long shopId = mapper.findActiveShopForUpdate(userId);
        if (shopId == null) {
            throw new CreatorShopUnavailableException();
        }
        ProductDraftInsert draft = new ProductDraftInsert(shopId, userId, request.title(), request.summary());
        // MyBatis 将第一张表生成的 productId 回填到 draft，第二次插入才能正确关联商品。
        // 保持 OFF_SALE + DRAFT，不更新已发布修订指针，公开目录不会把草稿当成商品。
        if (mapper.insertProduct(draft) != 1 || mapper.insertRevision(draft) != 1) {
            throw new IllegalStateException("Draft insertion did not affect exactly one row");
        }
        ProductDraftResponse response = mapper.findOwnedDraft(userId, draft.getProductId(), draft.getRevisionId());
        if (response == null) {
            throw new IllegalStateException("The newly created draft could not be loaded");
        }
        return response;
    }

    /** 返回自己的最新 50 条草稿；无草稿返回空数组，当前投递版未提供分页。 */
    @Transactional(readOnly = true)
    public List<ProductDraftResponse> listOwnDrafts(long userId) {
        return mapper.findOwnDrafts(userId);
    }
}
