package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchSuggestionResponseDTO {
    private List<String> keywords;
    private List<SearchUserResponseDTO> users;
    private List<SearchHashtagResponseDTO> hashtags;
    private List<RelatedSearchResponseDTO> relatedSearches;
}
