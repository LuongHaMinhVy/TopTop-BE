package com.back.follow.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.follow.model.dto.FollowingTrayResponseDTO;
import com.back.follow.service.IFollowingService;
import com.back.user.model.dto.response.UserInfo;
import com.back.video.model.dto.request.VideoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/following")
@RequiredArgsConstructor
public class FollowingController {

    private final IFollowingService followingService;

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getFollowingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> feedPage = followingService.getFollowingFeed(pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message("Following feed retrieved successfully")
                .data(feedPage.getContent())
                .meta(Meta.from(feedPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserInfo> suggestionsPage = followingService.getSuggestions(pageable);

        return ResponseEntity.ok(ApiResponse.<List<UserInfo>>builder()
                .message("Suggested users to follow retrieved successfully")
                .data(suggestionsPage.getContent())
                .meta(Meta.from(suggestionsPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/tray")
    public ResponseEntity<ApiResponse<FollowingTrayResponseDTO>> getTray() {
        FollowingTrayResponseDTO tray = followingService.getTray();

        return ResponseEntity.ok(ApiResponse.<FollowingTrayResponseDTO>builder()
                .message("Following tray retrieved successfully")
                .data(tray)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
