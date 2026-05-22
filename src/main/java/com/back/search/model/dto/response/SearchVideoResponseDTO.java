package com.back.search.model.dto.response;

import com.back.sound.model.dto.response.SoundResponseDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SearchVideoResponseDTO {
    private Long id;
    private String videoId;
    private String caption;
    private String coverUrl;
    private String videoUrl;
    private Long likeCount;
    private Long viewCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private SearchUserResponseDTO author;
    private SoundResponseDTO sound;
}
