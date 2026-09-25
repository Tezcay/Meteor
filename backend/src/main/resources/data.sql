INSERT IGNORE INTO shops (id, name, status)
VALUES (101, 'Northstar Design', 'ACTIVE');

INSERT IGNORE INTO products (id, shop_id, title, summary, status)
VALUES (1001, 101, '求职材料模板包', '包含简历、求职信与作品集排版模板。', 'PUBLISHED');

INSERT IGNORE INTO categories (slug, name, display_order, status)
VALUES ('design', '设计素材', 10, 'ACTIVE'),
       ('templates', '实用模板', 20, 'ACTIVE'),
       ('photography', '摄影', 30, 'ACTIVE'),
       ('illustration', '插画', 40, 'ACTIVE'),
       ('fonts', '字体', 50, 'ACTIVE'),
       ('three-d', '3D 素材', 60, 'ACTIVE'),
       ('audio', '音频', 70, 'ACTIVE'),
       ('publications', '电子书', 80, 'ACTIVE');

INSERT IGNORE INTO product_categories (product_id, category_slug)
VALUES (1001, 'templates');

INSERT IGNORE INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (2001, 1001, 'Basic', 1900, 'CNY', 'ON_SALE', 10),
       (2002, 1001, 'Pro', 3900, 'CNY', 'ON_SALE', 20);
