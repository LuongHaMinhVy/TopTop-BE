package com.back.chat.controller;

import com.back.chat.model.dto.request.CreateConversationRequestDTO;
import com.back.chat.model.dto.response.ConversationResponseDTO;
import com.back.chat.model.dto.response.UnreadCountResponseDTO;
import com.back.chat.service.IChatConversationService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/conversations")
@RequiredArgsConstructor
public class ChatConversationController {

    private final IChatConversationService chatConversationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponseDTO>>> getConversations(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Page<ConversationResponseDTO> page = chatConversationService.getMyConversations(authentication, pageable);

        return ResponseEntity.ok(ApiResponse.<List<ConversationResponseDTO>>builder()
                .message("Conversations retrieved successfully")
                .data(page.getContent())
                .meta(Meta.from(page))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<ConversationResponseDTO>> getOrCreateDirectConversation(
            @RequestBody CreateConversationRequestDTO request,
            Authentication authentication
    ) {
        ConversationResponseDTO data = chatConversationService.getOrCreateDirectConversation(authentication, request);

        return ResponseEntity.ok(ApiResponse.<ConversationResponseDTO>builder()
                .message("Conversation retrieved/created successfully")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatConversationService.markAsRead(authentication, conversationId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Conversation marked as read")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponseDTO>> getUnreadCount(Authentication authentication) {
        UnreadCountResponseDTO data = chatConversationService.getUnreadCount(authentication);

        return ResponseEntity.ok(ApiResponse.<UnreadCountResponseDTO>builder()
                .message("Unread count retrieved successfully")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
