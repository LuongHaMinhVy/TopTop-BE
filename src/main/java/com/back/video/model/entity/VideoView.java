package com.back.video.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(
        name = "video_views",
        indexes = {
                @Index(name = "idx_video_views_video_created", columnList = "video_id,created_at"),
                @Index(name = "idx_video_views_owner_created", columnList = "owner_id,created_at"),
                @Index(name = "idx_video_views_viewer_created", columnList = "viewer_id,created_at")
        }
)
public class VideoView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id")
    private User viewer;
}
