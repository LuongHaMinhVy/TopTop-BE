package com.back.video.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitVideoUploadRequestDTO {
    @NotBlank
    private String fileName;
    @NotBlank
    private String contentType;
    @Min(1)
    private long sizeBytes;
}
