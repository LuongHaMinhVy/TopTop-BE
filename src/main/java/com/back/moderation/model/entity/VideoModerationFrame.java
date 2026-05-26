package com.back.moderation.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.video.model.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "video_moderation_frames")
public class VideoModerationFrame extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderation_result_id")
    private VideoModerationResult moderationResult;

    @Column(name = "frame_index")
    private Integer frameIndex;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @Column(name = "frame_storage_key")
    private String frameStorageKey;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "categories_json", columnDefinition = "TEXT")
    private String categoriesJson;

    /** Comma-separated quality issues detected in this frame: WATERMARK, QR_CODE, LOW_QUALITY */
    @Column(name = "quality_issues_json", columnDefinition = "TEXT")
    private String qualityIssuesJson;
}
