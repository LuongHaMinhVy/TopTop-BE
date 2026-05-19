package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchHashtagResponseDTO {
    private Long id;
    private String name;
    private Long postCount;
}
