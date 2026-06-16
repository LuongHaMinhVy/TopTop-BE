package com.back.comment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAuthorResponseDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean verified;
}
