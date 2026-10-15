package dev.meteor.product.mapper.param;

/** 一次发布事务共用的参数；四个 setter 仅供 MyBatis 回填自增主键。 */
public class ProductPublicationInsert {
    private final long productId;
    private final long revisionId;
    private final long userId;
    private final String packageName;
    private final long priceCents;
    private final long fileObjectId;
    private final String summary;
    private final String filename;
    private long packageId;
    private long releaseId;
    private long manifestId;
    private long manifestItemId;

    public ProductPublicationInsert(long productId, long revisionId, long userId,
            String packageName, long priceCents, long fileObjectId, String summary, String filename) {
        this.productId = productId;
        this.revisionId = revisionId;
        this.userId = userId;
        this.packageName = packageName;
        this.priceCents = priceCents;
        this.fileObjectId = fileObjectId;
        this.summary = summary;
        this.filename = filename;
    }
    public long getProductId() { return productId; }
    public long getRevisionId() { return revisionId; }
    public long getUserId() { return userId; }
    public String getPackageName() { return packageName; }
    public long getPriceCents() { return priceCents; }
    public long getFileObjectId() { return fileObjectId; }
    public String getSummary() { return summary; }
    public String getFilename() { return filename; }
    public long getPackageId() { return packageId; }
    public void setPackageId(long packageId) { this.packageId = packageId; }
    public long getReleaseId() { return releaseId; }
    public void setReleaseId(long releaseId) { this.releaseId = releaseId; }
    public long getManifestId() { return manifestId; }
    public void setManifestId(long manifestId) { this.manifestId = manifestId; }
    public long getManifestItemId() { return manifestItemId; }
    public void setManifestItemId(long manifestItemId) { this.manifestItemId = manifestItemId; }
}
