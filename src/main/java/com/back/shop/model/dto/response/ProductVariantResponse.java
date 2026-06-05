package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String name;
    private String optionValues;
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;
}
