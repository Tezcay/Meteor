CREATE TABLE file_objects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    storage_provider VARCHAR(30) NOT NULL,
    storage_key VARCHAR(300) NOT NULL,
    original_filename VARCHAR(160) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_file_object_storage UNIQUE (storage_provider, storage_key),
    CONSTRAINT chk_file_object_size CHECK (size_bytes > 0),
    CONSTRAINT chk_file_object_sha256 CHECK (CHAR_LENGTH(content_sha256) = 64),
    INDEX idx_file_object_sha256 (content_sha256)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE delivery_manifests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    release_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_manifest_release FOREIGN KEY (release_id) REFERENCES releases (id),
    CONSTRAINT uq_delivery_manifest_release UNIQUE (release_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE manifest_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    manifest_id BIGINT NOT NULL,
    file_object_id BIGINT NOT NULL,
    item_position INT NOT NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_manifest_item_manifest FOREIGN KEY (manifest_id) REFERENCES delivery_manifests (id),
    CONSTRAINT fk_manifest_item_file_object FOREIGN KEY (file_object_id) REFERENCES file_objects (id),
    CONSTRAINT uq_manifest_item_position UNIQUE (manifest_id, item_position),
    CONSTRAINT uq_manifest_item_file UNIQUE (manifest_id, file_object_id),
    CONSTRAINT chk_manifest_item_position CHECK (item_position > 0),
    CONSTRAINT chk_manifest_item_role CHECK (role IN ('PRIMARY', 'GUIDE', 'BONUS')),
    INDEX idx_manifest_item_file_object (file_object_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO file_objects (
    id, storage_provider, storage_key, original_filename, media_type, size_bytes, content_sha256
) VALUES
    (5001, 'CLASSPATH', 'delivery-fixtures/basic/resume-template.html', 'basic-resume-template.html', 'text/html', 634, 'd56ccbeda6e7ca6610a9cb5b12fd7fb9231ec12787707b3577dfa87d56430f21'),
    (5002, 'CLASSPATH', 'delivery-fixtures/shared/usage-guide.md', 'usage-guide.md', 'text/markdown', 311, '69d43e8817bfc7206e1eb1bc58ac256bcc74a5ab8acdf0634d04b37e5aebf821'),
    (5003, 'CLASSPATH', 'delivery-fixtures/pro/resume-template.html', 'pro-resume-template.html', 'text/html', 775, 'e9755d9b1eea9b2451bcae3ec1dfaf231fd65aa86f536ece5fa53583272489fa'),
    (5004, 'CLASSPATH', 'delivery-fixtures/pro/cover-letter-template.md', 'cover-letter-template.md', 'text/markdown', 350, '43fc39d5f2c2b193649efe4037e25b51e6edc787846ba8ce2e3723eb78916996'),
    (5005, 'CLASSPATH', 'delivery-fixtures/pro/portfolio-outline.md', 'portfolio-outline.md', 'text/markdown', 397, '1050c7ef8bae95b2efb1d490e6dbd2bfef87651e70d7437217f67a34f001cf4b');

INSERT INTO delivery_manifests (id, release_id) VALUES
    (4001, 3001),
    (4002, 3002);

INSERT INTO manifest_items (
    id, manifest_id, file_object_id, item_position, role, display_name
) VALUES
    (6001, 4001, 5001, 1, 'PRIMARY', 'Basic 简历模板'),
    (6002, 4001, 5002, 2, 'GUIDE', '模板使用说明'),
    (6003, 4002, 5003, 1, 'PRIMARY', 'Pro 简历模板'),
    (6004, 4002, 5004, 2, 'BONUS', '求职信模板'),
    (6005, 4002, 5005, 3, 'BONUS', '作品集结构'),
    (6006, 4002, 5002, 4, 'GUIDE', '模板使用说明');
