package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ModerationQueueItemResponseDTO {
    private Long videoId;
    private String caption;
    private String coverUrl;
    private String authorUsername;
    private String authorAvatarUrl;
    private String moderationStatus;
    private Double riskScore;
    private List<String> categories;
    private Long reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime checkedAt;
}
