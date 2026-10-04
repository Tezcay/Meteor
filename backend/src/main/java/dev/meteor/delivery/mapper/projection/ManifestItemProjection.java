package dev.meteor.delivery.mapper.projection;

public record ManifestItemProjection(
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
