-- ============================================================
-- V20260601 — TikTok-style Livestream Feature
-- ============================================================

CREATE TABLE IF NOT EXISTS live_categories (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_live_category_slug (slug)
);

CREATE TABLE IF NOT EXISTS livestreams (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    host_id         BIGINT       NOT NULL,
    category_id     BIGINT       NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NULL,
    thumbnail_url   VARCHAR(500) NULL,
    status          ENUM('SCHEDULED','LIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    visibility      ENUM('PUBLIC','FOLLOWERS_ONLY','PRIVATE')    NOT NULL DEFAULT 'PUBLIC',
    allow_chat      TINYINT(1)   NOT NULL DEFAULT 1,
    allow_gifts     TINYINT(1)   NOT NULL DEFAULT 1,
    room_name       VARCHAR(200) NULL,
    viewer_count    INT          NOT NULL DEFAULT 0,
    peak_viewer_count INT        NOT NULL DEFAULT 0,
    like_count      BIGINT       NOT NULL DEFAULT 0,
    gift_count      BIGINT       NOT NULL DEFAULT 0,
    started_at      DATETIME     NULL,
    ended_at        DATETIME     NULL,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NULL,
    INDEX idx_livestream_status      (status),
    INDEX idx_livestream_host        (host_id),
    INDEX idx_livestream_started_at  (started_at),
    INDEX idx_livestream_visibility  (status, visibility)
);

CREATE TABLE IF NOT EXISTS livestream_participants (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id  BIGINT      NOT NULL,
    user_id        BIGINT      NOT NULL,
    role           ENUM('HOST','VIEWER','MODERATOR') NOT NULL DEFAULT 'VIEWER',
    joined_at      DATETIME    NULL,
    left_at        DATETIME    NULL,
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NULL,
    INDEX idx_live_participant_live (livestream_id),
    INDEX idx_live_participant_user (user_id)
);

CREATE TABLE IF NOT EXISTS livestream_chat_messages (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id  BIGINT        NOT NULL,
    sender_id      BIGINT        NOT NULL,
    message        VARCHAR(500)  NOT NULL,
    message_type   ENUM('CHAT','SYSTEM','GIFT','REACTION_SUMMARY','MODERATION') NOT NULL DEFAULT 'CHAT',
    is_pinned      TINYINT(1)    NOT NULL DEFAULT 0,
    is_hidden      TINYINT(1)    NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL,
    updated_at     DATETIME      NULL,
    INDEX idx_live_chat_live    (livestream_id, created_at),
    INDEX idx_live_chat_sender  (sender_id)
);

CREATE TABLE IF NOT EXISTS livestream_reactions (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id  BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    type           ENUM('LIKE') NOT NULL,
    count          INT          NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NULL,
    UNIQUE KEY uk_live_user_reaction (livestream_id, user_id, type),
    INDEX idx_live_reaction (livestream_id, type)
);

CREATE TABLE IF NOT EXISTS gift_catalog (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    icon_url       VARCHAR(500) NOT NULL,
    animation_url  VARCHAR(500) NULL,
    coin_price     INT          NOT NULL DEFAULT 0,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NULL
);

CREATE TABLE IF NOT EXISTS livestream_gifts (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id     BIGINT    NOT NULL,
    sender_id         BIGINT    NOT NULL,
    receiver_id       BIGINT    NOT NULL,
    gift_id           BIGINT    NOT NULL,
    quantity          INT       NOT NULL DEFAULT 1,
    total_coin_price  INT       NOT NULL DEFAULT 0,
    created_at        DATETIME  NOT NULL,
    INDEX idx_live_gifts      (livestream_id, created_at),
    INDEX idx_receiver_gifts  (receiver_id)
);

CREATE TABLE IF NOT EXISTS livestream_moderators (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id  BIGINT   NOT NULL,
    user_id        BIGINT   NOT NULL,
    created_by     BIGINT   NOT NULL,
    created_at     DATETIME NOT NULL,
    UNIQUE KEY uk_live_moderator (livestream_id, user_id)
);

CREATE TABLE IF NOT EXISTS livestream_bans (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id  BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    reason         VARCHAR(500) NULL,
    banned_by      BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL,
    UNIQUE KEY uk_live_banned_user (livestream_id, user_id)
);

CREATE TABLE IF NOT EXISTS livestream_moderation_logs (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    livestream_id     BIGINT       NOT NULL,
    moderator_id      BIGINT       NOT NULL,
    target_user_id    BIGINT       NULL,
    target_message_id BIGINT       NULL,
    action            VARCHAR(100) NOT NULL,
    reason            VARCHAR(500) NULL,
    created_at        DATETIME     NOT NULL,
    INDEX idx_live_mod_logs (livestream_id, created_at)
);

-- Seed default gift catalog
INSERT IGNORE INTO gift_catalog (name, icon_url, coin_price, is_active, created_at)
VALUES
    ('Rose',      'https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/rose.png',   1, 1, NOW()),
    ('Heart',     'https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/heart.png',  5, 1, NOW()),
    ('Star',      'https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/star.png',  10, 1, NOW()),
    ('Crown',     'https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/crown.png', 50, 1, NOW());

-- Seed default live categories
INSERT IGNORE INTO live_categories (name, slug, created_at)
VALUES
    ('Gaming',        'gaming',        NOW()),
    ('Music',         'music',         NOW()),
    ('Cooking',       'cooking',       NOW()),
    ('Fitness',       'fitness',       NOW()),
    ('Education',     'education',     NOW()),
    ('Entertainment', 'entertainment', NOW()),
    ('Sports',        'sports',        NOW()),
    ('Travel',        'travel',        NOW());
