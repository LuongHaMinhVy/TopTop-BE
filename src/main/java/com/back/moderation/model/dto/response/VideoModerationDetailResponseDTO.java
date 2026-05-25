package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VideoModerationDetailResponseDTO {
    private Long videoId;
    private String videoPreviewUrl;
    private String coverUrl;
    private String caption;
    private String authorUsername;
    private String authorAvatarUrl;
    private String moderationStatus;
    private Double riskScore;
    private Double textRiskScore;
    private Double imageRiskScore;
    private List<String> categories;
    private List<VideoModerationFrameResponseDTO> frames;
    private List<ModerationAuditLogResponseDTO> auditLogs;
    private Long reportCount;
    private LocalDateTime checkedAt;
}
