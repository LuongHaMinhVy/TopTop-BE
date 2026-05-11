package com.back.user.controller;

import com.back.auth.security.jwt.JwtService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
                .status(200)
                .build());
    }
}
