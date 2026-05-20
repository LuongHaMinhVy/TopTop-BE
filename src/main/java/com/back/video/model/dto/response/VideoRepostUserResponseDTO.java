package com.back.video.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRepostUserResponseDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Boolean isCurrentUser;
}
