package com.back.video.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitVideoUploadResponseDTO {
    private String uploadId;
    private String uploadUrl;
    private String objectKey;
    private String method;
}
