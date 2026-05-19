package com.back.search.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchUserResponseDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean verified;
    private Boolean followed;
    private Long followerCount;
    private Long totalLikeCount;
    private String bio;
}
