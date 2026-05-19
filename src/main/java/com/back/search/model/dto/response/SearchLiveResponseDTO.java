package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchLiveResponseDTO {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private Long viewerCount;
    private SearchUserResponseDTO host;
    private Boolean live;
}
