package com.back.chat.service;

import com.back.chat.model.dto.request.CreateConversationRequestDTO;
import com.back.chat.model.dto.response.ConversationResponseDTO;
import com.back.chat.model.dto.response.UnreadCountResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface IChatConversationService {
    Page<ConversationResponseDTO> getMyConversations(Authentication authentication, Pageable pageable);
    ConversationResponseDTO getOrCreateDirectConversation(Authentication authentication, CreateConversationRequestDTO request);
    void markAsRead(Authentication authentication, Long conversationId);
    UnreadCountResponseDTO getUnreadCount(Authentication authentication);
}
