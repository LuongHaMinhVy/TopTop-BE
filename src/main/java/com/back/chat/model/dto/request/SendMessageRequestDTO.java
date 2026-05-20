package com.back.chat.model.dto.request;

import com.back.chat.model.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequestDTO {

    @NotNull
    private Long conversationId;

    @NotNull
    private MessageType type;

    private String body;

    private Long videoId;

    private String mediaUrl;

    private String mediaType;

    private String fileName;

    private Long fileSize;

    private Long replyToMessageId;

    @NotBlank
    private String clientMessageId;
}
