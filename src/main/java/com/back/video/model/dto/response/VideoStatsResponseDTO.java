package com.back.video.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatsResponseDTO {
    private Long videoId;
    private Boolean liked;
    private Long likeCount;
    private Long commentCount;
    private Long saveCount;
    private Long shareCount;
}
