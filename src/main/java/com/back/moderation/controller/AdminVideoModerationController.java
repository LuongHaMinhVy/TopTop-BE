package com.back.moderation.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.moderation.model.dto.request.ReviewVideoModerationRequestDTO;
import com.back.moderation.model.dto.response.ModerationQueueItemResponseDTO;
import com.back.moderation.model.dto.response.VideoModerationDetailResponseDTO;
import com.back.moderation.model.dto.response.VideoModerationSummaryResponseDTO;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.moderation.service.IVideoModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/moderation/videos")
@RequiredArgsConstructor
public class AdminVideoModerationController {

    private final IVideoModerationService moderationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ModerationQueueItemResponseDTO>>> getQueue(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        VideoModerationStatus statusEnum = status != null ? VideoModerationStatus.valueOf(status) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<ModerationQueueItemResponseDTO> data = moderationService.getAdminQueue(statusEnum, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<ModerationQueueItemResponseDTO>>builder()
                .data(data)
                .message("Moderation queue loaded")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoModerationDetailResponseDTO>> getDetail(@PathVariable Long videoId) {
        VideoModerationDetailResponseDTO data = moderationService.getAdminDetail(videoId);
        return ResponseEntity.ok(ApiResponse.<VideoModerationDetailResponseDTO>builder()
                .data(data)
                .message("Moderation detail loaded")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{videoId}/review")
    public ResponseEntity<ApiResponse<VideoModerationSummaryResponseDTO>> reviewVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody ReviewVideoModerationRequestDTO request) {

        Long adminId = getCurrentAdminId();
        VideoModerationSummaryResponseDTO data = moderationService.reviewVideo(videoId, request, adminId);

        return ResponseEntity.ok(ApiResponse.<VideoModerationSummaryResponseDTO>builder()
                .data(data)
                .message("Review submitted")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<com.back.moderation.model.entity.ModerationAuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<com.back.moderation.model.entity.ModerationAuditLog> data = moderationService.getAuditLogs(pageable);

        return ResponseEntity.ok(ApiResponse.<Page<com.back.moderation.model.entity.ModerationAuditLog>>builder()
                .data(data)
                .message("Audit logs loaded")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        // Project extracts user from email; return null for system-level if unavailable
        return null;
    }
}
