package com.back.sound.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoundResponseDTO {
    private Long id;
    private String title;
    private String artistName;
    private String audioUrl;
    private String coverUrl;
    private Integer durationSeconds;
    private String type;
    private Boolean originalSound;
    private SoundAuthorResponseDTO owner;
    private SoundStatsResponseDTO stats;
    private Boolean isPublic;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
