package com.back.common.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.service.R2StorageService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    private final R2StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "context", defaultValue = "chat") String context
    ) throws IOException {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_REQUIRED);
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && IMAGE_TYPES.contains(contentType);
        boolean isVideo = contentType != null && VIDEO_TYPES.contains(contentType);
        if (!isImage && !isVideo) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        String safeContext = context == null || context.isBlank() ? "chat" : context.replaceAll("[^a-zA-Z0-9_-]", "");
        String key = "media/" + safeContext + "/" + UUID.randomUUID() + extension(file.getOriginalFilename(), isImage ? ".jpg" : ".mp4");
        String url = storageService.uploadFile(file, key);

        MediaUploadResponse data = MediaUploadResponse.builder()
                .url(url)
                .type(isImage ? "IMAGE" : "VIDEO")
                .contentType(contentType)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .build();

        return ResponseEntity.ok(ApiResponse.<MediaUploadResponse>builder()
                .message("Media uploaded successfully")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private String extension(String fileName, String fallback) {
        if (fileName == null) return fallback;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : fallback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaUploadResponse {
        private String url;
        private String type;
        private String contentType;
        private String fileName;
        private Long fileSize;
    }
}
