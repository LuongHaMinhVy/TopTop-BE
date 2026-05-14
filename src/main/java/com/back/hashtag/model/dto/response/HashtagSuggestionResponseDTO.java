package com.back.hashtag.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HashtagSuggestionResponseDTO {
    private Long id;
    private String name;
    private Long postCount;
    private String formattedPostCount;
}
