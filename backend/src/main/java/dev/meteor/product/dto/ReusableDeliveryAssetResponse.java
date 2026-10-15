package dev.meteor.product.dto;

/** 仅公开可选素材的展示信息，不把内部 storageKey 交给浏览器。 */
public record ReusableDeliveryAssetResponse(long id, String filename, String mediaType, long sizeBytes) {}
