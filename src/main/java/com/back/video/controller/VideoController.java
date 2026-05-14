package com.back.video.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoUploadRequestDTO;
import com.back.video.service.IVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.back.common.model.dto.response.Meta;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoService videoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponseDTO>> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart("data") @Valid VideoUploadRequestDTO requestDTO) throws IOException {
        VideoResponseDTO data = videoService.uploadVideo(file, cover, requestDTO);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("video.upload.success", "Video uploaded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> getVideoById(@PathVariable Long id) {
        VideoResponseDTO data = videoService.getVideoById(id);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("video.retrieve.success", "Video retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = videoService.getAllVideos(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.list.success", "Videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.builder()
                        .page(videoPage.getNumber())
                        .size(videoPage.getSize())
                        .totalPages(videoPage.getTotalPages())
                        .totalElements(videoPage.getTotalElements())
                        .build())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getVideosByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = videoService.getVideosByUserId(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.user_list.success", "User videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.builder()
                        .page(videoPage.getNumber())
                        .size(videoPage.getSize())
                        .totalPages(videoPage.getTotalPages())
                        .totalElements(videoPage.getTotalElements())
                        .build())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("video.delete.success", "Video deleted successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> reportVideo(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String reason = payload.getOrDefault("reason", "Inappropriate content");
        videoService.reportVideo(id, reason);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("video.report.success", "Video reported successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeVideo(@PathVariable Long id) {
        videoService.likeVideo(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("video.like.success", "Video liked successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeVideo(@PathVariable Long id) {
        videoService.unlikeVideo(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("video.unlike.success", "Video unliked successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/@{username}/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> getVideoByUsernameAndId(
            @PathVariable String username,
            @PathVariable Long videoId) {
        VideoResponseDTO data = videoService.getVideoByUsernameAndId(username, videoId);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("video.retrieve.success", "Video retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
