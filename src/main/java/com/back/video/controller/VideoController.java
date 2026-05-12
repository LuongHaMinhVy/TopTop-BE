package com.back.video.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.video.model.dto.VideoResponseDTO;
import com.back.video.model.dto.VideoUploadRequestDTO;
import com.back.video.service.IVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoService videoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponseDTO>> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") @Valid VideoUploadRequestDTO requestDTO) throws IOException {
        VideoResponseDTO data = videoService.uploadVideo(file, requestDTO);
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
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getAllVideos() {
        List<VideoResponseDTO> data = videoService.getAllVideos();
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.list.success", "Videos retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getVideosByUserId(@PathVariable Long userId) {
        List<VideoResponseDTO> data = videoService.getVideosByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.user_list.success", "User videos retrieved successfully"))
                .data(data)
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
}
