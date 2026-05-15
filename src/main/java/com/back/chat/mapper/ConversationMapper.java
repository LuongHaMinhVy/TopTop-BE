package com.back.chat.mapper;

import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.ConversationParticipant;
import com.back.chat.model.dto.response.ConversationResponseDTO;
import com.back.chat.model.dto.response.ConversationParticipantResponseDTO;
import com.back.user.model.entity.User;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConversationMapper {

    public static ConversationResponseDTO toResponse(Conversation conversation, ConversationParticipant currentParticipant, ConversationParticipant targetParticipant) {
        if (conversation == null) return null;

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .status(conversation.getStatus())
                .targetUser(toParticipantResponse(targetParticipant))
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(currentParticipant != null ? calculateUnreadCount(conversation, currentParticipant) : 0L)
                .muted(currentParticipant != null && currentParticipant.getMutedUntil() != null)
                .pinned(currentParticipant != null && currentParticipant.getIsPinned())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public static ConversationParticipantResponseDTO toParticipantResponse(ConversationParticipant participant) {
        if (participant == null || participant.getUser() == null) return null;
        User user = participant.getUser();
        return ConversationParticipantResponseDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .verified(user.getVerified())
                .online(false) // TODO: Implement online status
                .build();
    }

    private static Long calculateUnreadCount(Conversation conversation, ConversationParticipant participant) {
        if (conversation.getLastMessageAt() == null || participant.getLastReadAt() == null) {
            return conversation.getLastMessageAt() != null ? 1L : 0L;
        }
        return conversation.getLastMessageAt().isAfter(participant.getLastReadAt()) ? 1L : 0L; // Simplified
    }
}
