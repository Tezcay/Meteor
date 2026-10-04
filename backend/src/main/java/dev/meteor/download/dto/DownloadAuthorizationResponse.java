package dev.meteor.download.dto;

import java.time.Instant;

public record DownloadAuthorizationResponse(
        long ticketId,
        String downloadUrl,
        Instant expiresAt,
        String filename
) {
}
