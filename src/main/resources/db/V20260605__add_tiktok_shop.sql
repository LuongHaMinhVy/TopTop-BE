-- TikTok Shop MVP Migration
-- Phase 1: Core commerce tables

-- shops
CREATE TABLE shops (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) UNIQUE NOT NULL,
    description TEXT NULL,
    avatar_url VARCHAR(500) NULL,
    banner_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    moderation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_shops_owner_id(owner_id),
    INDEX idx_shops_status(status),
    INDEX idx_shops_moderation_status(moderation_status)
);

-- products
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    description TEXT NULL,
    category_id BIGINT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    stock_quantity INT NOT NULL DEFAULT 0,
    sold_count BIGINT NOT NULL DEFAULT 0,
    rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    moderation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_products_shop_slug(shop_id, slug),
    INDEX idx_products_shop_id(shop_id),
    INDEX idx_products_status_moderation(status, moderation_status),
    INDEX idx_products_category_id(category_id)
);

-- product_media
CREATE TABLE product_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    storage_key VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_product_media_product_id(product_id)
);

-- product_variants
CREATE TABLE product_variants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NULL,
    name VARCHAR(150) NOT NULL,
    option_values JSON NULL,
    price DECIMAL(12,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_product_variants_product_id(product_id)
);

-- video_product_links
CREATE TABLE video_product_links (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_video_product(video_id, product_id),
    INDEX idx_video_product_links_video_id(video_id),
    INDEX idx_video_product_links_product_id(product_id)
);

-- livestream_product_pins
CREATE TABLE livestream_product_pins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    pinned_by BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_live_product(livestream_id, product_id),
    INDEX idx_live_product_pins_live_id(livestream_id),
    INDEX idx_live_product_pins_product_id(product_id)
);

-- carts
CREATE TABLE carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL
);

-- cart_items
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    quantity INT NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_cart_product_variant(cart_id, product_id, variant_id),
    INDEX idx_cart_items_cart_id(cart_id)
);

-- orders
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    buyer_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    subtotal_amount DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    shipping_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SHIPPED',
    receiver_name VARCHAR(150) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    receiver_address TEXT NOT NULL,
    note TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_orders_buyer_id(buyer_id),
    INDEX idx_orders_shop_id(shop_id),
    INDEX idx_orders_status(status),
    INDEX idx_orders_payment_status(payment_status)
);

-- order_items
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    product_title VARCHAR(255) NOT NULL,
    variant_name VARCHAR(150) NULL,
    product_image_url VARCHAR(500) NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_order_items_order_id(order_id)
);

-- payments
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'COD',
    provider_transaction_id VARCHAR(150) NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_payments_order_id(order_id),
    INDEX idx_payments_status(status)
);

-- product_reviews
CREATE TABLE product_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_review_order_item(order_item_id),
    INDEX idx_reviews_product_id(product_id),
    INDEX idx_reviews_user_id(user_id)
);
