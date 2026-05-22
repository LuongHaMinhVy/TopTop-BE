package com.back.notification.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private String type;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
    
    private Long actorId;
    private String actorUsername;
    private String actorAvatarUrl;
    
    private Long videoId;
    private String videoOwnerUsername;
    private String videoThumbnailUrl;
}
