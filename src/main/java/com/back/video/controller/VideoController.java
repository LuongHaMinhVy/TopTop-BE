package com.back.video.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.request.InitVideoUploadRequestDTO;
import com.back.video.model.dto.request.CompleteVideoUploadRequestDTO;
import com.back.video.model.dto.response.InitVideoUploadResponseDTO;
import com.back.video.model.dto.response.VideoDailyMetricResponseDTO;
import com.back.video.model.dto.response.VideoStatsResponseDTO;
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
import com.back.common.utils.redis.RateLimit;

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
    @RateLimit(limit = 10, durationInSeconds = 300)
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

    @PostMapping("/init")
    @RateLimit(limit = 10, durationInSeconds = 300)
    public ResponseEntity<ApiResponse<InitVideoUploadResponseDTO>> initUpload(@Valid @RequestBody InitVideoUploadRequestDTO requestDTO) {
        InitVideoUploadResponseDTO data = videoService.initVideoUpload(requestDTO);
        return ResponseEntity.ok(ApiResponse.<InitVideoUploadResponseDTO>builder()
                .message("Upload session initialized")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(limit = 10, durationInSeconds = 300)
    public ResponseEntity<ApiResponse<VideoResponseDTO>> completeUpload(
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart("data") @Valid CompleteVideoUploadRequestDTO requestDTO) {
        VideoResponseDTO data = videoService.completeVideoUpload(requestDTO, cover);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message("Video upload completed")
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
                .meta(Meta.from(videoPage))
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
                .meta(Meta.from(videoPage))
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
    @RateLimit(limit = 5, durationInSeconds = 60)
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
    @RateLimit(limit = 30, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<VideoStatsResponseDTO>> likeVideo(@PathVariable Long id) {
        VideoStatsResponseDTO data = videoService.likeVideo(id);
        return ResponseEntity.ok(ApiResponse.<VideoStatsResponseDTO>builder()
                .message(Translator.toLocale("video.like.success", "Video liked successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<VideoStatsResponseDTO>> unlikeVideo(@PathVariable Long id) {
        VideoStatsResponseDTO data = videoService.unlikeVideo(id);
        return ResponseEntity.ok(ApiResponse.<VideoStatsResponseDTO>builder()
                .message(Translator.toLocale("video.unlike.success", "Video unliked successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/repost")
    @RateLimit(limit = 30, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<VideoStatsResponseDTO>> repostVideo(@PathVariable Long id) {
        VideoStatsResponseDTO data = videoService.repostVideo(id);
        return ResponseEntity.ok(ApiResponse.<VideoStatsResponseDTO>builder()
                .message(Translator.toLocale("video.repost.success", "Video reposted successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}/repost")
    public ResponseEntity<ApiResponse<VideoStatsResponseDTO>> unrepostVideo(@PathVariable Long id) {
        VideoStatsResponseDTO data = videoService.unrepostVideo(id);
        return ResponseEntity.ok(ApiResponse.<VideoStatsResponseDTO>builder()
                .message(Translator.toLocale("video.unrepost.success", "Video repost removed successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/view")
    @RateLimit(limit = 50, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<VideoStatsResponseDTO>> recordVideoView(@PathVariable Long id) {
        VideoStatsResponseDTO data = videoService.recordVideoView(id);
        return ResponseEntity.ok(ApiResponse.<VideoStatsResponseDTO>builder()
                .message(Translator.toLocale("video.view.success", "Video view recorded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/studio/analytics/views")
    public ResponseEntity<ApiResponse<List<VideoDailyMetricResponseDTO>>> getStudioDailyViews(
            @RequestParam(defaultValue = "7") int days) {
        List<VideoDailyMetricResponseDTO> data = videoService.getStudioDailyViews(days);
        return ResponseEntity.ok(ApiResponse.<List<VideoDailyMetricResponseDTO>>builder()
                .message(Translator.toLocale("video.analytics.views.success", "Video view analytics retrieved successfully"))
                .data(data)
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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> updateVideo(
            @PathVariable Long id,
            @RequestBody @Valid com.back.video.model.dto.response.VideoUploadRequestDTO requestDTO) {
        VideoResponseDTO data = videoService.updateVideo(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message("Video updated successfully")
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getLikedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = videoService.getLikedVideos(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.liked_list.success", "Liked videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/users/{username}/reposts")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getRepostedVideosByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = videoService.getRepostedVideosByUsername(username, pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("video.reposted_list.success", "Reposted videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
