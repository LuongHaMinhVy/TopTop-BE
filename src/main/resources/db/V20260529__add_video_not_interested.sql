CREATE TABLE IF NOT EXISTS video_not_interested (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_video_not_interested_user_video UNIQUE (user_id, video_id),
    CONSTRAINT fk_video_not_interested_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_video_not_interested_video FOREIGN KEY (video_id) REFERENCES videos (id)
);

CREATE INDEX idx_video_not_interested_user ON video_not_interested (user_id);
CREATE INDEX idx_video_not_interested_video ON video_not_interested (video_id);
