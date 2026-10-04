package dev.meteor.delivery.dto;

public record ManifestItemResponse(
        long id,
        int position,
        String role,
        String displayName,
        String filename,
        String mediaType,
        long sizeBytes,
        String sha256
) {
}
