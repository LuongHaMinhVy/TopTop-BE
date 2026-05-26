package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VideoModerationSummaryResponseDTO {
    private Long videoId;
    private String moderationStatus;
    private Double riskScore;
    private String reasonCode;
    private String reasonMessage;
    private LocalDateTime checkedAt;
    private String musicCopyrightStatus;
    private String musicCopyrightReasonCode;
    private String musicCopyrightReasonMessage;
    private LocalDateTime musicCopyrightCheckedAt;
    /** Quality issues detected across video frames: WATERMARK, QR_CODE, LOW_QUALITY */
    private List<String> qualityIssues;
    /** Human-readable Vietnamese summary of quality issues, null if none found */
    private String qualityIssueMessage;
}

