CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT chk_user_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_user_role CHECK (role IN ('USER', 'CREATOR', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO users (id, email, password_hash, display_name, status)
VALUES (1, 'creator@meteor.local', NULL, 'Northstar Creator', 'INVITED');

INSERT INTO user_roles (user_id, role)
VALUES (1, 'USER'), (1, 'CREATOR');

ALTER TABLE products DROP FOREIGN KEY fk_product_shop;
ALTER TABLE product_categories DROP FOREIGN KEY fk_product_category_product;
ALTER TABLE packages DROP FOREIGN KEY fk_package_product;

ALTER TABLE shops
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN owner_user_id BIGINT NULL AFTER id,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;

UPDATE shops SET owner_user_id = 1 WHERE owner_user_id IS NULL;

ALTER TABLE shops
    MODIFY owner_user_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_shop_owner UNIQUE (owner_user_id),
    ADD CONSTRAINT fk_shop_owner FOREIGN KEY (owner_user_id) REFERENCES users (id);

ALTER TABLE products
    ADD CONSTRAINT fk_product_shop FOREIGN KEY (shop_id) REFERENCES shops (id);

ALTER TABLE products
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN sale_status VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE' AFTER shop_id,
    ADD COLUMN current_published_revision_id BIGINT NULL AFTER sale_status,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;

UPDATE products
SET sale_status = CASE status
    WHEN 'PUBLISHED' THEN 'ON_SALE'
    ELSE 'OFF_SALE'
END;

ALTER TABLE packages
    ADD CONSTRAINT fk_package_product FOREIGN KEY (product_id) REFERENCES products (id);

CREATE TABLE product_revisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_revision_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_revision_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT uq_revision_product_number UNIQUE (product_id, revision_number),
    CONSTRAINT chk_revision_number CHECK (revision_number > 0),
    CONSTRAINT chk_revision_status CHECK (
        status IN ('DRAFT', 'IN_REVIEW', 'REJECTED', 'PUBLISHED', 'BLOCKED')
    ),
    INDEX idx_revision_product_status (product_id, status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO product_revisions (
    product_id,
    revision_number,
    title,
    summary,
    status,
    created_by_user_id,
    created_at,
    published_at
)
SELECT p.id,
       1,
       p.title,
       p.summary,
       CASE WHEN p.status IN ('PUBLISHED', 'OFF_SALE') THEN 'PUBLISHED' ELSE 'DRAFT' END,
       s.owner_user_id,
       p.created_at,
       CASE WHEN p.status IN ('PUBLISHED', 'OFF_SALE') THEN p.created_at ELSE NULL END
FROM products p
JOIN shops s ON s.id = p.shop_id;

UPDATE products p
JOIN product_revisions pr
  ON pr.product_id = p.id
 AND pr.revision_number = 1
 AND pr.status = 'PUBLISHED'
SET p.current_published_revision_id = pr.id;

ALTER TABLE products
    ADD CONSTRAINT chk_product_sale_status CHECK (sale_status IN ('ON_SALE', 'OFF_SALE')),
    ADD CONSTRAINT fk_product_published_revision
        FOREIGN KEY (current_published_revision_id) REFERENCES product_revisions (id);

CREATE TABLE taxonomy_terms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    term_type VARCHAR(20) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_taxonomy_parent FOREIGN KEY (parent_id) REFERENCES taxonomy_terms (id),
    CONSTRAINT uq_taxonomy_slug UNIQUE (slug),
    CONSTRAINT chk_taxonomy_type CHECK (term_type IN ('CATEGORY', 'SUBCATEGORY')),
    CONSTRAINT chk_taxonomy_status CHECK (status IN ('ACTIVE', 'HIDDEN')),
    INDEX idx_taxonomy_parent_order (parent_id, status, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO taxonomy_terms (parent_id, term_type, slug, name, display_order, status)
SELECT NULL, 'CATEGORY', slug, name, display_order, status
FROM categories;

CREATE TABLE product_revision_taxonomy_terms (
    product_revision_id BIGINT NOT NULL,
    taxonomy_term_id BIGINT NOT NULL,
    PRIMARY KEY (product_revision_id, taxonomy_term_id),
    CONSTRAINT fk_revision_taxonomy_revision
        FOREIGN KEY (product_revision_id) REFERENCES product_revisions (id),
    CONSTRAINT fk_revision_taxonomy_term
        FOREIGN KEY (taxonomy_term_id) REFERENCES taxonomy_terms (id),
    INDEX idx_revision_taxonomy_term (taxonomy_term_id, product_revision_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO product_revision_taxonomy_terms (product_revision_id, taxonomy_term_id)
SELECT pr.id, tt.id
FROM product_categories pc
JOIN product_revisions pr
  ON pr.product_id = pc.product_id
 AND pr.revision_number = 1
JOIN taxonomy_terms tt ON tt.slug = pc.category_slug;

DROP TABLE product_categories;
DROP TABLE categories;

ALTER TABLE products
    DROP INDEX idx_product_shop_status,
    DROP CHECK chk_product_status,
    DROP COLUMN title,
    DROP COLUMN summary,
    DROP COLUMN status,
    ADD INDEX idx_product_shop_sale (shop_id, sale_status, created_at);

ALTER TABLE packages
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;
