package com.back.search.model.dto.response;

import com.back.search.model.enums.SearchSourceType;
import com.back.search.model.enums.SearchType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SearchHistoryResponseDTO {
    private Long id;
    private String keyword;
    private SearchType type;
    private SearchSourceType sourceType;
    private Long resultTargetId;
    private LocalDateTime searchedAt;
}
