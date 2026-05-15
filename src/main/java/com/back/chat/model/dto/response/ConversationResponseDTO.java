package com.back.chat.model.dto.response;

import com.back.chat.model.enums.ConversationStatus;
import com.back.chat.model.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponseDTO {
    private Long id;
    private ConversationType type;
    private ConversationStatus status;
    private ConversationParticipantResponseDTO targetUser;
    private MessageResponseDTO lastMessage;
    private String lastMessagePreview;
    private Long unreadCount;
    private Boolean muted;
    private Boolean pinned;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
