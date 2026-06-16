package com.back.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContentFilterTagRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String tag;

    private String sampleThumbnailUrl;
}
