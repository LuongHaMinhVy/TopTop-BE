package com.back.livestream.model.dto.response;

import com.back.livestream.model.enums.ChatMessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveChatMessageResponse {
    private Long id;
    private Long livestreamId;
    private ChatMessageType type;
    private SenderSummary sender;
    private String message;
    private boolean isPinned;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class SenderSummary {
        private Long id;
        private String username;
        private String displayName;
        private String avatarUrl;
    }
}
