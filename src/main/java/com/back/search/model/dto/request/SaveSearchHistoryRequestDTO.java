package com.back.search.model.dto.request;

import com.back.search.model.enums.SearchSourceType;
import com.back.search.model.enums.SearchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveSearchHistoryRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String keyword;

    private SearchType type = SearchType.ALL;

    private SearchSourceType sourceType = SearchSourceType.KEYWORD;

    private Long resultTargetId;
}
