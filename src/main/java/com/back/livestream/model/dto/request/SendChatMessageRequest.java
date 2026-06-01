package com.back.livestream.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendChatMessageRequest {
    @NotBlank
    @Size(max = 500)
    private String message;
}
