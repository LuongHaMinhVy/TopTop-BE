package com.back.shop.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotNull
    private Long productId;

    private Long variantId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
