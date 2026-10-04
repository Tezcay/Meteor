package dev.meteor.delivery.dto;

import java.time.Instant;
import java.util.List;

public record DeliveryManifestResponse(
        long id,
        long releaseId,
        String versionLabel,
        Instant createdAt,
        int itemCount,
        long totalBytes,
        List<ManifestItemResponse> items
) {
}
