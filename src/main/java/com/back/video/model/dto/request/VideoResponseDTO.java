package com.back.video.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String thumbnailUrl;
    private Integer duration;
    private String category;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long saveCount;
    private Long userId;
    private String username;
    private String userNickname;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
    private Boolean isSaved;
}
