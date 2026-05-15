package com.back.chat.mapper;

import com.back.chat.model.entity.Message;
import com.back.chat.model.entity.MessageAttachment;
import com.back.chat.model.dto.response.MessageResponseDTO;
import com.back.chat.model.dto.response.MessageAttachmentResponseDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageMapper {

    public static MessageResponseDTO toResponse(Message message, MessageAttachment attachment, Long currentUserId) {
        if (message == null) return null;

        return MessageResponseDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .type(message.getType())
                .body(message.getBody())
                .status(message.getStatus())
                .attachment(toAttachmentResponse(attachment))
                .replyToMessageId(message.getReplyToMessageId())
                .clientMessageId(message.getClientMessageId())
                .mine(message.getSender().getId().equals(currentUserId))
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    public static MessageAttachmentResponseDTO toAttachmentResponse(MessageAttachment attachment) {
        if (attachment == null) return null;

        return MessageAttachmentResponseDTO.builder()
                .type(attachment.getType())
                .videoId(attachment.getVideoId())
                .videoUrl(attachment.getUrl())
                .thumbnailUrl(attachment.getThumbnailUrl())
                .build();
    }
}
