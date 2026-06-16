package com.back.collection.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(
        name = "saved_videos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_videos_user_video", columnNames = {"user_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_saved_videos_user", columnList = "user_id"),
                @Index(name = "idx_saved_videos_video", columnList = "video_id")
        }
)
public class SavedVideo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
