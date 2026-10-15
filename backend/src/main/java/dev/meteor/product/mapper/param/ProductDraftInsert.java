package dev.meteor.product.mapper.param;

/**
 * 两次 INSERT 共用的参数。使用普通 class 而不是 record，是因为 MyBatis
 * 要通过 setter 回填两个数据库自增主键；客户端不会接触这个对象。
 */
public class ProductDraftInsert {
    private long productId;
    private long revisionId;
    private final long shopId;
    private final long userId;
    private final String title;
    private final String summary;

    public ProductDraftInsert(long shopId, long userId, String title, String summary) {
        this.shopId = shopId;
        this.userId = userId;
        this.title = title;
        this.summary = summary;
    }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public long getRevisionId() { return revisionId; }
    public void setRevisionId(long revisionId) { this.revisionId = revisionId; }
    public long getShopId() { return shopId; }
    public long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
}
