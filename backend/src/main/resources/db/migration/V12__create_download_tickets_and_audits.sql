CREATE TABLE download_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    entitlement_grant_id BIGINT NOT NULL,
    release_id BIGINT NOT NULL,
    manifest_item_id BIGINT NOT NULL,
    file_object_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_download_ticket_hash UNIQUE (token_hash),
    CONSTRAINT fk_download_ticket_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_download_ticket_grant FOREIGN KEY (entitlement_grant_id) REFERENCES entitlement_grants (id),
    CONSTRAINT fk_download_ticket_release FOREIGN KEY (release_id) REFERENCES releases (id),
    CONSTRAINT fk_download_ticket_manifest_item FOREIGN KEY (manifest_item_id) REFERENCES manifest_items (id),
    CONSTRAINT fk_download_ticket_file_object FOREIGN KEY (file_object_id) REFERENCES file_objects (id),
    CONSTRAINT chk_download_ticket_status CHECK (status IN ('ISSUED', 'CONSUMED')),
    CONSTRAINT chk_download_ticket_consumed CHECK (
        (status = 'ISSUED' AND consumed_at IS NULL)
        OR (status = 'CONSUMED' AND consumed_at IS NOT NULL)
    ),
    CONSTRAINT chk_download_ticket_expiry CHECK (expires_at > created_at),
    INDEX idx_download_ticket_user_status_expiry (user_id, status, expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE download_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    download_ticket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entitlement_grant_id BIGINT NOT NULL,
    release_id BIGINT NOT NULL,
    manifest_item_id BIGINT NOT NULL,
    file_object_id BIGINT NOT NULL,
    outcome VARCHAR(40) NOT NULL,
    response_bytes BIGINT NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_download_audit_ticket UNIQUE (download_ticket_id),
    CONSTRAINT fk_download_audit_ticket FOREIGN KEY (download_ticket_id) REFERENCES download_tickets (id),
    CONSTRAINT fk_download_audit_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_download_audit_grant FOREIGN KEY (entitlement_grant_id) REFERENCES entitlement_grants (id),
    CONSTRAINT fk_download_audit_release FOREIGN KEY (release_id) REFERENCES releases (id),
    CONSTRAINT fk_download_audit_manifest_item FOREIGN KEY (manifest_item_id) REFERENCES manifest_items (id),
    CONSTRAINT fk_download_audit_file_object FOREIGN KEY (file_object_id) REFERENCES file_objects (id),
    CONSTRAINT chk_download_audit_outcome CHECK (outcome IN ('FILE_RESPONSE_PREPARED')),
    CONSTRAINT chk_download_audit_bytes CHECK (response_bytes > 0),
    CONSTRAINT chk_download_audit_sha256 CHECK (CHAR_LENGTH(content_sha256) = 64),
    INDEX idx_download_audit_user_occurred (user_id, occurred_at, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
