-- Seed products for seller/shop id = 2
-- Schema note:
-- products table uses shop_id, not seller_id.
-- This file assumes products.shop_id = 2.
-- If shops.id = 2 does not exist, the first INSERT will create it.
-- If users.id = 2 does not exist, the shop INSERT may fail because shops.owner_id should reference a real seller/user in your logic.

START TRANSACTION;

-- Optional: create shop id = 2 if it does not exist.
-- Remove this block if you already have shops.id = 2.
INSERT INTO shops
(
    id,
    created_at,
    updated_at,
    avatar_url,
    banner_url,
    description,
    moderation_status,
    name,
    owner_id,
    slug,
    status
)
VALUES
(
    2,
    NOW(6),
    NOW(6),
    NULL,
    NULL,
    'Shop bán sản phẩm thời trang, phụ kiện và đồ dùng cá nhân.',
    'APPROVED',
    'Vy Store',
    2,
    'vy-store',
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    updated_at = NOW(6),
    name = VALUES(name),
    description = VALUES(description),
    moderation_status = VALUES(moderation_status),
    status = VALUES(status);

-- Products for shop_id = 2
INSERT INTO products
(
    created_at,
    updated_at,
    base_price,
    category_id,
    currency,
    description,
    is_deleted,
    moderation_status,
    rating_avg,
    rating_count,
    shop_id,
    slug,
    sold_count,
    status,
    stock_quantity,
    title
)
VALUES
(
    NOW(6),
    NOW(6),
    199000.00,
    NULL,
    'VND',
    'Áo thun basic form rộng, chất cotton mềm, phù hợp mặc hằng ngày.',
    b'0',
    'APPROVED',
    4.80,
    125,
    2,
    'ao-thun-basic-form-rong-den',
    340,
    'ACTIVE',
    120,
    'Áo thun basic form rộng màu đen'
),
(
    NOW(6),
    NOW(6),
    349000.00,
    NULL,
    'VND',
    'Quần jogger unisex chất nỉ dày vừa, co giãn nhẹ, phong cách streetwear.',
    b'0',
    'APPROVED',
    4.70,
    89,
    2,
    'quan-jogger-unisex-streetwear',
    210,
    'ACTIVE',
    80,
    'Quần jogger unisex streetwear'
),
(
    NOW(6),
    NOW(6),
    99000.00,
    NULL,
    'VND',
    'Ốp lưng điện thoại trong suốt chống sốc, viền dẻo, bảo vệ camera.',
    b'0',
    'APPROVED',
    4.60,
    57,
    2,
    'op-lung-trong-suot-chong-soc',
    500,
    'ACTIVE',
    300,
    'Ốp lưng trong suốt chống sốc'
),
(
    NOW(6),
    NOW(6),
    459000.00,
    NULL,
    'VND',
    'Tai nghe bluetooth không dây, hộp sạc nhỏ gọn, âm thanh ổn định.',
    b'0',
    'APPROVED',
    4.50,
    43,
    2,
    'tai-nghe-bluetooth-khong-day-mini',
    150,
    'ACTIVE',
    60,
    'Tai nghe bluetooth không dây mini'
),
(
    NOW(6),
    NOW(6),
    129000.00,
    NULL,
    'VND',
    'Bình nước thể thao dung tích 700ml, nhựa an toàn, có dây xách tiện lợi.',
    b'0',
    'APPROVED',
    4.90,
    76,
    2,
    'binh-nuoc-the-thao-700ml',
    260,
    'ACTIVE',
    180,
    'Bình nước thể thao 700ml'
);

-- Product variants
INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    'Size M - Đen',
    JSON_OBJECT('size', 'M', 'color', 'Đen'),
    199000.00,
    p.id,
    'TSHIRT-BASIC-BLACK-M',
    'ACTIVE',
    40
FROM products p
WHERE p.slug = 'ao-thun-basic-form-rong-den';

INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    'Size L - Đen',
    JSON_OBJECT('size', 'L', 'color', 'Đen'),
    199000.00,
    p.id,
    'TSHIRT-BASIC-BLACK-L',
    'ACTIVE',
    50
FROM products p
WHERE p.slug = 'ao-thun-basic-form-rong-den';

INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    'Màu đen',
    JSON_OBJECT('color', 'Đen'),
    349000.00,
    p.id,
    'JOGGER-STREET-BLACK',
    'ACTIVE',
    80
FROM products p
WHERE p.slug = 'quan-jogger-unisex-streetwear';

INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    'iPhone 13',
    JSON_OBJECT('model', 'iPhone 13', 'color', 'Trong suốt'),
    99000.00,
    p.id,
    'CASE-CLEAR-IP13',
    'ACTIVE',
    100
FROM products p
WHERE p.slug = 'op-lung-trong-suot-chong-soc';

INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    'Màu trắng',
    JSON_OBJECT('color', 'Trắng'),
    459000.00,
    p.id,
    'EARBUDS-MINI-WHITE',
    'ACTIVE',
    60
FROM products p
WHERE p.slug = 'tai-nghe-bluetooth-khong-day-mini';

INSERT INTO product_variants
(
    created_at,
    updated_at,
    name,
    option_values,
    price,
    product_id,
    sku,
    status,
    stock_quantity
)
SELECT
    NOW(6),
    NOW(6),
    '700ml - Xanh dương',
    JSON_OBJECT('capacity', '700ml', 'color', 'Xanh dương'),
    129000.00,
    p.id,
    'BOTTLE-700-BLUE',
    'ACTIVE',
    180
FROM products p
WHERE p.slug = 'binh-nuoc-the-thao-700ml';

-- Product media
INSERT INTO product_media
(
    created_at,
    media_type,
    product_id,
    sort_order,
    storage_key,
    url
)
SELECT
    NOW(6),
    'IMAGE',
    p.id,
    1,
    CONCAT('products/', p.slug, '/main.jpg'),
    CONCAT('https://example.com/images/products/', p.slug, '.jpg')
FROM products p
WHERE p.slug IN (
    'ao-thun-basic-form-rong-den',
    'quan-jogger-unisex-streetwear',
    'op-lung-trong-suot-chong-soc',
    'tai-nghe-bluetooth-khong-day-mini',
    'binh-nuoc-the-thao-700ml'
);

COMMIT;
