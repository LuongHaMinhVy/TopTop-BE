package com.back.sound.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.sound.model.enums.SoundType;
import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sounds", indexes = {
        @Index(name = "idx_sounds_type_active", columnList = "type,is_active,is_deleted"),
        @Index(name = "idx_sounds_owner", columnList = "owner_id"),
        @Index(name = "idx_sounds_source_video", columnList = "source_video_id"),
        @Index(name = "idx_sounds_usage_count", columnList = "usage_count"),
        @Index(name = "idx_sounds_title", columnList = "title")
})
public class Sound extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 150)
    private String artistName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SoundType type;

    @Column(nullable = false, length = 1000)
    private String audioUrl;

    @Column(length = 500)
    private String audioStorageKey;

    @Column(length = 1000)
    private String coverUrl;

    @Column(length = 500)
    private String coverStorageKey;

    @Column(nullable = false)
    private Integer durationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_video_id")
    private Video sourceVideo;

    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Builder.Default
    @Column(name = "saved_count", nullable = false)
    private Long savedCount = 0L;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
