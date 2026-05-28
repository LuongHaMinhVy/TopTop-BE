package com.back.user.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContentFilterTagResponseDTO {
    private Long id;
    private String tag;
    private String sampleThumbnailUrl;
    private LocalDateTime createdAt;
}
