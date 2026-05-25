package com.back.moderation.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ModerationAuditLogResponseDTO {
    private String action;
    private String actorType;
    private Long actorUserId;
    private String previousStatus;
    private String newStatus;
    private String reasonCode;
    private String reasonMessage;
    private LocalDateTime createdAt;
}
