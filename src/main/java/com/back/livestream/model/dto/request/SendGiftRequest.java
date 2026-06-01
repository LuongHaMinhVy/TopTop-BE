package com.back.livestream.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendGiftRequest {
    @NotNull
    private Long giftId;

    @Min(1)
    private int quantity = 1;
}
