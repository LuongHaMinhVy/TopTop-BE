package com.back.shop.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {
    @NotBlank
    private String name;

    private String sku;
    private String optionValues;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @Min(0)
    private Integer stockQuantity = 0;
}
