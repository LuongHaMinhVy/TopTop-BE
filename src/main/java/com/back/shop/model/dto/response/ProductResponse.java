package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private Long shopId;
    private String title;
    private String slug;
    private String description;
    private Long categoryId;
    private BigDecimal basePrice;
    private String currency;
    private Integer stockQuantity;
    private Long soldCount;
    private BigDecimal ratingAvg;
    private Long ratingCount;
    private String status;
    private String moderationStatus;
    private List<ProductMediaResponse> media;
    private List<ProductVariantResponse> variants;
}
