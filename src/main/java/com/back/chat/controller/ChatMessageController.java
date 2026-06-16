package com.back.chat.controller;

import com.back.chat.model.dto.request.SendMessageRequestDTO;
import com.back.chat.model.dto.response.MessageResponseDTO;
import com.back.chat.service.IChatMessageService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.redis.RateLimit;
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
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final IChatMessageService chatMessageService;

    @PostMapping("/messages")
    @RateLimit(limit = 20, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<MessageResponseDTO>> sendMessage(
            @RequestBody SendMessageRequestDTO request,
            Authentication authentication
    ) {
        MessageResponseDTO data = chatMessageService.sendMessage(authentication, request);

        return ResponseEntity.ok(ApiResponse.<MessageResponseDTO>builder()
                .message("Message sent successfully")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponseDTO>>> getMessages(
            @PathVariable Long conversationId,
            @PageableDefault(size = 30) Pageable pageable,
            Authentication authentication
    ) {
        Page<MessageResponseDTO> page = chatMessageService.getMessages(authentication, conversationId, pageable);

        return ResponseEntity.ok(ApiResponse.<List<MessageResponseDTO>>builder()
                .message("Messages retrieved successfully")
                .data(page.getContent())
                .meta(Meta.from(page))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        chatMessageService.deleteMessage(authentication, messageId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Message deleted successfully")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
