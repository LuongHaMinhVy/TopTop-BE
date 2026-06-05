package com.back.shop.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    private Long categoryId;

    @DecimalMin("0.01")
    private BigDecimal basePrice;

    @Min(0)
    private Integer stockQuantity;

    private String currency;

    private List<ProductMediaRequest> media;
    private List<ProductVariantRequest> variants;
}
