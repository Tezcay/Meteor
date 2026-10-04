package dev.meteor.download.mapper.projection;

public record DownloadableFileProjection(
        long grantId,
        long releaseId,
        long manifestId,
        long manifestItemId,
        long fileObjectId,
        String storageProvider,
        String storageKey,
        String originalFilename,
        String mediaType,
        long sizeBytes,
        String contentSha256
) {
}
