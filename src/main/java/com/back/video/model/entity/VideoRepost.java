package com.back.video.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(
        name = "video_reposts",
        uniqueConstraints = @UniqueConstraint(name = "uk_video_reposts_user_video", columnNames = {"user_id", "video_id"}),
        indexes = {
                @Index(name = "idx_video_reposts_user", columnList = "user_id"),
                @Index(name = "idx_video_reposts_video", columnList = "video_id")
        }
)
public class VideoRepost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
