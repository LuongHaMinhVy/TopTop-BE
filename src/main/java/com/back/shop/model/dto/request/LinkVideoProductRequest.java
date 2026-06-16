package com.back.shop.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LinkVideoProductRequest {
    @NotNull
    private List<Long> productIds;
}
