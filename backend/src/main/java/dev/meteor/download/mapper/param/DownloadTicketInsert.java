package dev.meteor.download.mapper.param;

import dev.meteor.download.model.DownloadTicketStatus;
import java.time.Instant;

public final class DownloadTicketInsert {

    private long id;
    private final String tokenHash;
    private final long userId;
    private final long grantId;
    private final long releaseId;
    private final long manifestItemId;
    private final long fileObjectId;
    private final DownloadTicketStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    public DownloadTicketInsert(
            String tokenHash,
            long userId,
            long grantId,
            long releaseId,
            long manifestItemId,
            long fileObjectId,
            DownloadTicketStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.grantId = grantId;
        this.releaseId = releaseId;
        this.manifestItemId = manifestItemId;
        this.fileObjectId = fileObjectId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public long getUserId() {
        return userId;
    }

    public long getGrantId() {
        return grantId;
    }

    public long getReleaseId() {
        return releaseId;
    }

    public long getManifestItemId() {
        return manifestItemId;
    }

    public long getFileObjectId() {
        return fileObjectId;
    }

    public DownloadTicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
