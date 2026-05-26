package com.back.moderation.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.moderation.model.enums.ModerationDecision;
import com.back.moderation.model.enums.ModerationProviderType;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.video.model.entity.Video;
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
@Table(name = "video_moderation_results")
public class VideoModerationResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationProviderType provider;

    @Column(name = "provider_version")
    private String providerVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoModerationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_decision")
    private ModerationDecision finalDecision;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "text_risk_score")
    private Double textRiskScore;

    @Column(name = "image_risk_score")
    private Double imageRiskScore;

    @Column(name = "sampled_frame_count")
    private Integer sampledFrameCount;

    @Column(name = "categories_json", columnDefinition = "TEXT")
    private String categoriesJson;

    /** Aggregated quality issues across all frames: WATERMARK, QR_CODE, LOW_QUALITY */
    @Column(name = "quality_issues_json", columnDefinition = "TEXT")
    private String qualityIssuesJson;

    @Column(name = "raw_result_json", columnDefinition = "TEXT")
    private String rawResultJson;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_message", columnDefinition = "TEXT")
    private String reasonMessage;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;
}
