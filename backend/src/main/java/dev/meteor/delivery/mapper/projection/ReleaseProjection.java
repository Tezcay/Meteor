package dev.meteor.delivery.mapper.projection;

import java.time.Instant;

public record ReleaseProjection(
        long id,
        long packageId,
        int releaseNumber,
        String versionLabel,
        String summary,
        Instant publishedAt
) {
}
