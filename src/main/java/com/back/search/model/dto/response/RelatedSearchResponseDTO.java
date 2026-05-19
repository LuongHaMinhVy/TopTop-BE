package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelatedSearchResponseDTO {
    private String keyword;
    private Long searchCount;
}
