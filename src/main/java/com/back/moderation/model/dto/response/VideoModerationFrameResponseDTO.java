package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VideoModerationFrameResponseDTO {
    private Integer frameIndex;
    private Long timestampMs;
    private Double riskScore;
    private String categoriesJson;
}
