package com.back.follow.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.follow.service.IFriendService;
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
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final IFriendService friendService;

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getFriendsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> feedPage = friendService.getFriendsFeed(pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("friend.feed.success", "Friends feed retrieved successfully"))
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
        Page<UserInfo> suggestionsPage = friendService.getSuggestions(pageable);

        return ResponseEntity.ok(ApiResponse.<List<UserInfo>>builder()
                .message(Translator.toLocale("friend.suggestions.success", "Suggested friends retrieved successfully"))
                .data(suggestionsPage.getContent())
                .meta(Meta.from(suggestionsPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countFriends() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message(Translator.toLocale("friend.count.success", "Friends count retrieved successfully"))
                .data(friendService.countFriends())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
