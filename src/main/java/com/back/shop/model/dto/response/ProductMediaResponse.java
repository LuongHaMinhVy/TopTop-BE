package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductMediaResponse {
    private Long id;
    private String url;
    private String mediaType;
    private Integer sortOrder;
}
