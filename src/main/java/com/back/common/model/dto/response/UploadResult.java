package com.back.common.model.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadResult {
    private String url;
    private String publicId;
}