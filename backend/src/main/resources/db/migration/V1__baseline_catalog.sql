CREATE TABLE shops (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_shop_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    CONSTRAINT chk_product_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFF_SALE')),
    INDEX idx_product_shop_status (shop_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    slug VARCHAR(50) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (name),
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'HIDDEN')),
    INDEX idx_category_status_order (status, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_categories (
    product_id BIGINT NOT NULL,
    category_slug VARCHAR(50) NOT NULL,
    PRIMARY KEY (product_id, category_slug),
    CONSTRAINT fk_product_category_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_category_category FOREIGN KEY (category_slug) REFERENCES categories (slug),
    INDEX idx_product_category_slug (category_slug, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE packages (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    price_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    sale_status VARCHAR(20) NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_package_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_package_product_name UNIQUE (product_id, name),
    CONSTRAINT chk_package_price CHECK (price_cents >= 0),
    CONSTRAINT chk_package_currency CHECK (currency = 'CNY'),
    CONSTRAINT chk_package_sale_status CHECK (sale_status IN ('ON_SALE', 'STOPPED')),
    INDEX idx_package_product_sale (product_id, sale_status, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO shops (id, name, status)
VALUES (101, 'Northstar Design', 'ACTIVE');

INSERT INTO products (id, shop_id, title, summary, status)
VALUES (1001, 101, '求职材料模板包', '包含简历、求职信与作品集排版模板。', 'PUBLISHED');

INSERT INTO categories (slug, name, display_order, status)
VALUES ('design', '设计素材', 10, 'ACTIVE'),
       ('templates', '实用模板', 20, 'ACTIVE'),
       ('photography', '摄影', 30, 'ACTIVE'),
       ('illustration', '插画', 40, 'ACTIVE'),
       ('fonts', '字体', 50, 'ACTIVE'),
       ('three-d', '3D 素材', 60, 'ACTIVE'),
       ('audio', '音频', 70, 'ACTIVE'),
       ('publications', '电子书', 80, 'ACTIVE');

INSERT INTO product_categories (product_id, category_slug)
VALUES (1001, 'templates');

INSERT INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (2001, 1001, 'Basic', 1900, 'CNY', 'ON_SALE', 10),
       (2002, 1001, 'Pro', 3900, 'CNY', 'ON_SALE', 20);
