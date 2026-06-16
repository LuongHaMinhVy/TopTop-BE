package com.back.video.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.moderation.model.enums.MusicCopyrightStatus;
import com.back.hashtag.model.entity.Hashtag;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.sound.model.entity.Sound;
import com.back.user.model.entity.User;
import com.back.video.model.enums.VideoVisibility;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "videos")
public class Video extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String fileUrl;

    @Column
    private String thumbnailUrl;

    @Column
    private Integer duration;

    @Column
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_category_id")
    private VideoCategory videoCategory;


    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long likeCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long commentCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long saveCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoVisibility visibility = VideoVisibility.PUBLIC;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowComments = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowEdit = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sound_id")
    private Sound sound;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "video_hashtags",
        joinColumns = @JoinColumn(name = "video_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private java.util.Set<Hashtag> hashtags = new java.util.HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    @Builder.Default
    private VideoModerationStatus moderationStatus = VideoModerationStatus.PENDING;

    @Column(name = "moderation_checked_at")
    private LocalDateTime moderationCheckedAt;

    @Column(name = "moderation_reason_code")
    private String moderationReasonCode;

    @Column(name = "moderation_reason_message", columnDefinition = "TEXT")
    private String moderationReasonMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "music_copyright_status", nullable = false)
    @Builder.Default
    private MusicCopyrightStatus musicCopyrightStatus = MusicCopyrightStatus.PENDING;

    @Column(name = "music_copyright_checked_at")
    private LocalDateTime musicCopyrightCheckedAt;

    @Column(name = "music_copyright_reason_code")
    private String musicCopyrightReasonCode;

    @Column(name = "music_copyright_reason_message", columnDefinition = "TEXT")
    private String musicCopyrightReasonMessage;

    @Column(name = "quality_issues_json", columnDefinition = "TEXT")
    private String qualityIssuesJson;

    @Column(name = "quality_issue_message", columnDefinition = "TEXT")
    private String qualityIssueMessage;

    @Column(name = "edit_instructions_json", columnDefinition = "TEXT")
    private String editInstructionsJson;

    @Column(name = "ai_category")
    private String aiCategory;

    @Column(name = "ai_tags_json", columnDefinition = "TEXT")
    private String aiTagsJson;

    @Column(name = "detected_language")
    private String detectedLanguage;

    @Column(name = "content_mood")
    private String contentMood;

    @Column(name = "content_quality_score")
    private Double contentQualityScore;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
