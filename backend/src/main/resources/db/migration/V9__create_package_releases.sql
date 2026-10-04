CREATE TABLE releases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    release_number INT NOT NULL,
    version_label VARCHAR(40) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    published_at TIMESTAMP(6) NOT NULL,
    disabled_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_release_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT uq_release_package_number UNIQUE (package_id, release_number),
    CONSTRAINT uq_release_package_label UNIQUE (package_id, version_label),
    CONSTRAINT chk_release_number CHECK (release_number > 0),
    CONSTRAINT chk_release_status CHECK (status IN ('PUBLISHED', 'DISABLED')),
    CONSTRAINT chk_release_disabled_at CHECK (
        (status = 'PUBLISHED' AND disabled_at IS NULL)
        OR (status = 'DISABLED' AND disabled_at IS NOT NULL)
    ),
    INDEX idx_release_package_status_published (package_id, status, published_at, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO releases (
    id,
    package_id,
    release_number,
    version_label,
    summary,
    status,
    published_at
) VALUES
    (3001, 2001, 1, '1.0.0', 'Basic 首个正式版本：简历与求职信基础版式及使用说明。', 'PUBLISHED', CURRENT_TIMESTAMP(6)),
    (3002, 2002, 1, '1.0.0', 'Pro 首个正式版本：完整求职材料与作品集版式及使用说明。', 'PUBLISHED', CURRENT_TIMESTAMP(6));
