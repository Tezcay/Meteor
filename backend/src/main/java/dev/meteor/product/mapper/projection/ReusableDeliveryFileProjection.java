package dev.meteor.product.mapper.projection;

/** 通过当前店铺的正式清单取得；文件对象本身没有 owner 字段，不能只按 ID 授权。 */
public record ReusableDeliveryFileProjection(
        long id, String filename, String mediaType, long sizeBytes,
        String storageKey, String contentSha256
) {}
