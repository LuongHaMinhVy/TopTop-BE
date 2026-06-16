package com.back.chat.model.dto.response;

import com.back.chat.model.enums.MessageStatus;
import com.back.chat.model.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private MessageType type;
    private String body;
    private MessageStatus status;
    private MessageAttachmentResponseDTO attachment;
    private Long replyToMessageId;
    private String clientMessageId;
    private Boolean mine;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
