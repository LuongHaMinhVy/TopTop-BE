package com.back.video.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(
        name = "video_not_interested",
        uniqueConstraints = @UniqueConstraint(name = "uk_video_not_interested_user_video", columnNames = {"user_id", "video_id"}),
        indexes = {
                @Index(name = "idx_video_not_interested_user", columnList = "user_id"),
                @Index(name = "idx_video_not_interested_video", columnList = "video_id")
        }
)
public class VideoNotInterested extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
