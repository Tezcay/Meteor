package dev.meteor.download.mapper.param;

import dev.meteor.download.model.DownloadAuditOutcome;

public record DownloadAuditInsert(
        long ticketId,
        long userId,
        long grantId,
        long releaseId,
        long manifestItemId,
        long fileObjectId,
        DownloadAuditOutcome outcome,
        long responseBytes,
        String contentSha256
) {
}
