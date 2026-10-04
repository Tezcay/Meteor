package dev.meteor.download.mapper.projection;

import dev.meteor.download.model.DownloadTicketStatus;
import java.time.Instant;

public record DownloadTicketProjection(
        long id,
        long userId,
        long grantId,
        long releaseId,
        long manifestItemId,
        long fileObjectId,
        DownloadTicketStatus status,
        Instant expiresAt,
        Instant consumedAt
) {
}
