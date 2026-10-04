package dev.meteor.delivery.dto;

import java.time.Instant;

public record ReleaseResponse(
        long id,
        long packageId,
        int releaseNumber,
        String versionLabel,
        String summary,
        Instant publishedAt,
        boolean latest
) {
}
