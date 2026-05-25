package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
}
