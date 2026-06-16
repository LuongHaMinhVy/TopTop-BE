package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private Long variantId;
    private String productTitle;
    private String variantName;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
}
