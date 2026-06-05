package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productImageUrl;
    private Long variantId;
    private String variantName;
    private BigDecimal price;
    private Integer quantity;
    private Boolean selected;
    private Integer stockQuantity;
    private Boolean isAvailable;
}
