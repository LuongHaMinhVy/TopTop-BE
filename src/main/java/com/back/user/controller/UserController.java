package com.back.user.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController{
    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(HttpServletRequest request) {
        UserInfo userInfo = userService.getUserInfo(request);
        return ResponseEntity.ok(ApiResponse.<UserInfo>builder()
                .message("Success")
                .data(userInfo)
                .status(HttpStatus.OK.value())
                .build());
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserInfo>> getUserProfile(@PathVariable String username) {
        UserInfo userInfo = userService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.<UserInfo>builder()
                .message("Success")
                .data(userInfo)
                .status(HttpStatus.OK.value())
                .build());
    }

    @GetMapping("/mentions")
    public ResponseEntity<ApiResponse<java.util.List<com.back.user.model.dto.response.MentionSuggestionResponseDTO>>> getMentions(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        java.util.List<com.back.user.model.dto.response.MentionSuggestionResponseDTO> data = userService.getMentionSuggestions(keyword);
        return ResponseEntity.ok(ApiResponse.<java.util.List<com.back.user.model.dto.response.MentionSuggestionResponseDTO>>builder()
                .message(com.back.common.utils.Translator.toLocale("user.mention.success", "Mention suggestions retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .build());
    }
}
