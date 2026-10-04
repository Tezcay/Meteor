package dev.meteor.delivery.mapper.projection;

import java.time.Instant;

public record DeliveryManifestProjection(
        long id,
        long releaseId,
        String versionLabel,
        Instant createdAt
) {
}
