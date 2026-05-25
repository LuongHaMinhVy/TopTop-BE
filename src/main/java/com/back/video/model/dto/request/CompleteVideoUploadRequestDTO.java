package com.back.video.model.dto.request;

import com.back.video.model.dto.response.VideoUploadRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompleteVideoUploadRequestDTO extends VideoUploadRequestDTO {
    @NotBlank
    private String uploadId;
}
