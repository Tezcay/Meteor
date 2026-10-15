-- Imported showcase fixtures use reserved IDs 9101..9104 in each existing table.
-- No INSERT IGNORE / upsert: any ID collision must fail visibly rather than overwrite user records.
-- Demonstration prices and shop attribution do not establish original authorship or commercial licenses.
-- ZIP files stay in the backend classpath, outside public web assets.

INSERT INTO products (id, shop_id, sale_status) VALUES (9101, 101, 'OFF_SALE');
INSERT INTO product_revisions (id, product_id, revision_number, title, summary, status, created_by_user_id, published_at)
VALUES (9101, 9101, 1, '光与空间', '3 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', 1, CURRENT_TIMESTAMP(6));
INSERT INTO product_revision_taxonomy_terms (product_revision_id, taxonomy_term_id)
SELECT 9101, id FROM taxonomy_terms WHERE slug = 'photography' AND status = 'ACTIVE';
INSERT INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (9101, 9101, '图集 ZIP', 1900, 'CNY', 'ON_SALE', 10);
INSERT INTO releases (id, package_id, release_number, version_label, summary, status, published_at)
VALUES (9101, 9101, 1, '1.0.0', '3 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', CURRENT_TIMESTAMP(6));
INSERT INTO file_objects (id, storage_provider, storage_key, original_filename, media_type, size_bytes, content_sha256)
VALUES (9101, 'CLASSPATH', 'delivery-fixtures/showcase/light-and-space.zip', 'light-and-space.zip', 'application/zip', 352288, '2419ab368fd30c87a05ed3c97959d0316543f31af415fe3245e0e46f90fd66c1');
INSERT INTO delivery_manifests (id, release_id) VALUES (9101, 9101);
INSERT INTO manifest_items (id, manifest_id, file_object_id, item_position, role, display_name)
VALUES (9101, 9101, 9101, 1, 'PRIMARY', '光与空间图集 ZIP');
UPDATE products SET current_published_revision_id = 9101, sale_status = 'ON_SALE' WHERE id = 9101;

INSERT INTO products (id, shop_id, sale_status) VALUES (9102, 101, 'OFF_SALE');
INSERT INTO product_revisions (id, product_id, revision_number, title, summary, status, created_by_user_id, published_at)
VALUES (9102, 9102, 1, '冬日静谧', '3 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', 1, CURRENT_TIMESTAMP(6));
INSERT INTO product_revision_taxonomy_terms (product_revision_id, taxonomy_term_id)
SELECT 9102, id FROM taxonomy_terms WHERE slug = 'illustration' AND status = 'ACTIVE';
INSERT INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (9102, 9102, '图集 ZIP', 2900, 'CNY', 'ON_SALE', 10);
INSERT INTO releases (id, package_id, release_number, version_label, summary, status, published_at)
VALUES (9102, 9102, 1, '1.0.0', '3 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', CURRENT_TIMESTAMP(6));
INSERT INTO file_objects (id, storage_provider, storage_key, original_filename, media_type, size_bytes, content_sha256)
VALUES (9102, 'CLASSPATH', 'delivery-fixtures/showcase/winter-stillness.zip', 'winter-stillness.zip', 'application/zip', 499632, '818e7379a3877fecd3264c3312921e6fca86ab6772b213c8d39df4e5ea63337f');
INSERT INTO delivery_manifests (id, release_id) VALUES (9102, 9102);
INSERT INTO manifest_items (id, manifest_id, file_object_id, item_position, role, display_name)
VALUES (9102, 9102, 9102, 1, 'PRIMARY', '冬日静谧图集 ZIP');
UPDATE products SET current_published_revision_id = 9102, sale_status = 'ON_SALE' WHERE id = 9102;

INSERT INTO products (id, shop_id, sale_status) VALUES (9103, 101, 'OFF_SALE');
INSERT INTO product_revisions (id, product_id, revision_number, title, summary, status, created_by_user_id, published_at)
VALUES (9103, 9103, 1, '静谧建筑', '1 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', 1, CURRENT_TIMESTAMP(6));
INSERT INTO product_revision_taxonomy_terms (product_revision_id, taxonomy_term_id)
SELECT 9103, id FROM taxonomy_terms WHERE slug = 'illustration' AND status = 'ACTIVE';
INSERT INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (9103, 9103, '图集 ZIP', 1900, 'CNY', 'ON_SALE', 10);
INSERT INTO releases (id, package_id, release_number, version_label, summary, status, published_at)
VALUES (9103, 9103, 1, '1.0.0', '1 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', CURRENT_TIMESTAMP(6));
INSERT INTO file_objects (id, storage_provider, storage_key, original_filename, media_type, size_bytes, content_sha256)
VALUES (9103, 'CLASSPATH', 'delivery-fixtures/showcase/quiet-architecture.zip', 'quiet-architecture.zip', 'application/zip', 112142, '3b3e257cad7bf52efa4da9de534cd8b27652722e3c02fb118344eed84950f120');
INSERT INTO delivery_manifests (id, release_id) VALUES (9103, 9103);
INSERT INTO manifest_items (id, manifest_id, file_object_id, item_position, role, display_name)
VALUES (9103, 9103, 9103, 1, 'PRIMARY', '静谧建筑图集 ZIP');
UPDATE products SET current_published_revision_id = 9103, sale_status = 'ON_SALE' WHERE id = 9103;

INSERT INTO products (id, shop_id, sale_status) VALUES (9104, 101, 'OFF_SALE');
INSERT INTO product_revisions (id, product_id, revision_number, title, summary, status, created_by_user_id, published_at)
VALUES (9104, 9104, 1, '风景叙事', '2 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', 1, CURRENT_TIMESTAMP(6));
INSERT INTO product_revision_taxonomy_terms (product_revision_id, taxonomy_term_id)
SELECT 9104, id FROM taxonomy_terms WHERE slug = 'illustration' AND status = 'ACTIVE';
INSERT INTO packages (id, product_id, name, price_cents, currency, sale_status, display_order)
VALUES (9104, 9104, '图集 ZIP', 2900, 'CNY', 'ON_SALE', 10);
INSERT INTO releases (id, package_id, release_number, version_label, summary, status, published_at)
VALUES (9104, 9104, 1, '1.0.0', '2 张站点已有 WebP 图片与素材说明；演示价格，不含源文件或新增商用授权。', 'PUBLISHED', CURRENT_TIMESTAMP(6));
INSERT INTO file_objects (id, storage_provider, storage_key, original_filename, media_type, size_bytes, content_sha256)
VALUES (9104, 'CLASSPATH', 'delivery-fixtures/showcase/landscape-narrative.zip', 'landscape-narrative.zip', 'application/zip', 496336, '17292a861572719e5c94ef9e8ce5f59beb1afa436ce0489a877b85158aaf975d');
INSERT INTO delivery_manifests (id, release_id) VALUES (9104, 9104);
INSERT INTO manifest_items (id, manifest_id, file_object_id, item_position, role, display_name)
VALUES (9104, 9104, 9104, 1, 'PRIMARY', '风景叙事图集 ZIP');
UPDATE products SET current_published_revision_id = 9104, sale_status = 'ON_SALE' WHERE id = 9104;
