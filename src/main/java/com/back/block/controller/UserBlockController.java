package com.back.block.controller;

import com.back.block.service.IUserBlockService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.user.model.dto.response.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class UserBlockController {

    private final IUserBlockService userBlockService;

    @PostMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable String username) {
        userBlockService.blockUser(username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("block.success", "User blocked successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable String username) {
        userBlockService.unblockUser(username);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("unblock.success", "User unblocked successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserInfo>>> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserInfo> blockedPage = userBlockService.getBlockedUsers(pageable);

        return ResponseEntity.ok(ApiResponse.<List<UserInfo>>builder()
                .message(Translator.toLocale("block.list.success", "Blocked users retrieved successfully"))
                .data(blockedPage.getContent())
                .meta(Meta.from(blockedPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
