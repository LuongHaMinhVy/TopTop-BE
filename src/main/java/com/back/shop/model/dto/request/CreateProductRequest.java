package com.back.shop.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    private Long categoryId;

    @DecimalMin("0.01")
    private BigDecimal basePrice;

    @Min(0)
    private Integer stockQuantity = 0;

    private String currency = "VND";

    private List<ProductMediaRequest> media;
    private List<ProductVariantRequest> variants;
}
