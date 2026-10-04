ALTER TABLE order_items
    ADD COLUMN shop_name VARCHAR(100) NULL AFTER product_title,
    ADD COLUMN access_policy VARCHAR(30) NULL AFTER package_name;

UPDATE order_items oi
JOIN products p ON p.id = oi.product_id
JOIN shops s ON s.id = p.shop_id
SET oi.shop_name = s.name,
    oi.access_policy = 'ALL_RELEASES'
WHERE oi.shop_name IS NULL
   OR oi.access_policy IS NULL;

ALTER TABLE order_items
    MODIFY shop_name VARCHAR(100) NOT NULL,
    MODIFY access_policy VARCHAR(30) NOT NULL,
    ADD CONSTRAINT chk_order_item_access_policy
        CHECK (access_policy IN ('ALL_RELEASES'));

CREATE TABLE entitlement_grants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    access_policy VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    granted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_entitlement_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_entitlement_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_entitlement_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_entitlement_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_entitlement_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT chk_entitlement_access_policy CHECK (access_policy IN ('ALL_RELEASES')),
    CONSTRAINT chk_entitlement_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT chk_entitlement_revocation CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
    ),
    INDEX idx_entitlement_user_status_granted (user_id, status, granted_at, id),
    INDEX idx_entitlement_product_package (product_id, package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
