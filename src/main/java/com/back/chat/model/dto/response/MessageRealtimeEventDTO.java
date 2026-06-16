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
public class MessageRealtimeEventDTO {
    private String event;
    private Long conversationId;
    private MessageResponseDTO message;
    private ConversationResponseDTO conversation;
    private LocalDateTime occurredAt;
}
