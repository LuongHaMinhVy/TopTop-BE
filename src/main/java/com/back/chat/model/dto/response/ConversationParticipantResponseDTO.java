package com.back.chat.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipantResponseDTO {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean verified;
    private Boolean online;
    private LocalDateTime lastActiveAt;
}
