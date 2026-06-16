package com.back.moderation.model.dto.request;

import com.back.moderation.model.enums.ModerationDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewVideoModerationRequestDTO {
    @NotNull
    private ModerationDecision decision;

    @Size(max = 100)
    private String reasonCode;

    @Size(max = 1000)
    private String reasonMessage;
}
