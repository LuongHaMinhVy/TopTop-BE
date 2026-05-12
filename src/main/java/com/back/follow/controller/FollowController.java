package com.back.follow.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.follow.service.IFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    @PostMapping("/{username}")
    public ApiResponse<Void> follow(@PathVariable String username) {
        followService.followUser(username);
        return ApiResponse.<Void>builder()
                .message("Followed successfully")
                .build();
    }

    @DeleteMapping("/{username}")
    public ApiResponse<Void> unfollow(@PathVariable String username) {
        followService.unfollowUser(username);
        return ApiResponse.<Void>builder()
                .message("Unfollowed successfully")
                .build();
    }
}
