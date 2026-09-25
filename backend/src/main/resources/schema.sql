CREATE TABLE IF NOT EXISTS shops (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_shop_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS products (
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

CREATE TABLE IF NOT EXISTS categories (
    slug VARCHAR(50) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (name),
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'HIDDEN')),
    INDEX idx_category_status_order (status, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_categories (
    product_id BIGINT NOT NULL,
    category_slug VARCHAR(50) NOT NULL,
    PRIMARY KEY (product_id, category_slug),
    CONSTRAINT fk_product_category_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_category_category FOREIGN KEY (category_slug) REFERENCES categories (slug),
    INDEX idx_product_category_slug (category_slug, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS packages (
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
