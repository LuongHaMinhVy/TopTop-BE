package com.back.chat.mapper;

import com.back.chat.model.entity.Message;
import com.back.chat.model.entity.MessageAttachment;
import com.back.chat.model.dto.response.MessageResponseDTO;
import com.back.chat.model.dto.response.MessageAttachmentResponseDTO;
import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageMapper {

    private static final Pattern TITLE_PATTERN = Pattern.compile("\"title\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern OWNER_USERNAME_PATTERN = Pattern.compile("\"ownerUsername\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("\"fileName\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern FILE_SIZE_PATTERN = Pattern.compile("\"fileSize\"\\s*:\\s*(\\d+)");

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
                .url(attachment.getUrl())
                .thumbnailUrl(attachment.getThumbnailUrl())
                .fileName(readMetadataValue(attachment.getMetadataJson(), FILE_NAME_PATTERN))
                .fileSize(readLongMetadataValue(attachment.getMetadataJson(), FILE_SIZE_PATTERN))
                .title(readMetadataValue(attachment.getMetadataJson(), TITLE_PATTERN))
                .ownerUsername(readMetadataValue(attachment.getMetadataJson(), OWNER_USERNAME_PATTERN))
                .build();
    }

    private static String readMetadataValue(String metadataJson, Pattern pattern) {
        if (metadataJson == null || metadataJson.isBlank()) return null;

        Matcher matcher = pattern.matcher(metadataJson);
        if (!matcher.find()) return null;

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    private static Long readLongMetadataValue(String metadataJson, Pattern pattern) {
        if (metadataJson == null || metadataJson.isBlank()) return null;

        Matcher matcher = pattern.matcher(metadataJson);
        if (!matcher.find()) return null;

        return Long.parseLong(matcher.group(1));
    }
}
