package com.back.user.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MentionSuggestionResponseDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean verified;
}
