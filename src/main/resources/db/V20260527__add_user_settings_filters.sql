ALTER TABLE users
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE TABLE IF NOT EXISTS user_content_filter_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    tag VARCHAR(80) NOT NULL,
    sample_thumbnail_url VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_content_filter_tag UNIQUE (user_id, tag),
    CONSTRAINT fk_user_content_filter_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_content_filter_user ON user_content_filter_tags (user_id);
