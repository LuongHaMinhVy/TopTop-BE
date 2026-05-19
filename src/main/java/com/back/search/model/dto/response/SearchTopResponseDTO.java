package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchTopResponseDTO {
    private List<SearchVideoResponseDTO> videos;
    private List<SearchUserResponseDTO> users;
    private List<SearchHashtagResponseDTO> hashtags;
    private List<SearchLiveResponseDTO> lives;
    private List<RelatedSearchResponseDTO> relatedSearches;
}
