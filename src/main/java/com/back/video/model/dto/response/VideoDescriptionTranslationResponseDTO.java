package com.back.video.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDescriptionTranslationResponseDTO {
    private Long videoId;
    private String sourceText;
    private String translatedText;
    private String targetLocale;
}
