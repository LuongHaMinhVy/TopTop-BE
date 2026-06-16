package com.back.shop.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductMediaRequest {
    @NotBlank
    private String url;

    private String storageKey;

    @NotNull
    private String mediaType; // IMAGE or VIDEO

    private Integer sortOrder = 0;
}
