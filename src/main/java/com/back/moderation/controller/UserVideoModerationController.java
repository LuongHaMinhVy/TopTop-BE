package com.back.moderation.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.moderation.model.dto.response.VideoModerationSummaryResponseDTO;
import com.back.moderation.service.IVideoModerationService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class UserVideoModerationController {

    private final IVideoModerationService moderationService;
    private final IUserRepo userRepo;

    @GetMapping("/{videoId}/moderation-status")
    public ResponseEntity<ApiResponse<VideoModerationSummaryResponseDTO>> getModerationStatus(
            @PathVariable Long videoId) {

        User user = getCurrentUser();
        Long requesterId = user != null ? user.getId() : null;
        boolean isAdmin = isAdmin();

        VideoModerationSummaryResponseDTO data = moderationService.getModerationStatus(videoId, requesterId, isAdmin);

        return ResponseEntity.ok(ApiResponse.<VideoModerationSummaryResponseDTO>builder()
                .data(data)
                .message("Moderation status loaded")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return null;
        return userRepo.findByEmail(auth.getName()).orElse(null);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
