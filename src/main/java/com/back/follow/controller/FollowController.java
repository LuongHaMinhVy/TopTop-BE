package com.back.follow.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.follow.service.IFollowService;
import com.back.user.model.dto.response.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.common.model.dto.response.Meta;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    @PostMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> follow(@PathVariable String username) {
        followService.followUser(username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("follow.success", "Followed successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable String username) {
        followService.unfollowUser(username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("unfollow.success", "Unfollowed successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getFollowingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserInfo> followPage = followService.getFollowingList(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<UserInfo>>builder()
                .message(Translator.toLocale("follow.list.success", "Following list retrieved successfully"))
                .data(followPage.getContent())
                .meta(Meta.from(followPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}

