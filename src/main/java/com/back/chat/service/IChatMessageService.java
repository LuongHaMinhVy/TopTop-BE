package com.back.chat.service;

import com.back.chat.model.dto.request.SendMessageRequestDTO;
import com.back.chat.model.dto.response.MessageResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface IChatMessageService {
    MessageResponseDTO sendMessage(Authentication authentication, SendMessageRequestDTO request);
    Page<MessageResponseDTO> getMessages(Authentication authentication, Long conversationId, Pageable pageable);
}
