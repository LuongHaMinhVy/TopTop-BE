package com.back.auth.controller;

import com.back.auth.model.dto.request.LoginRequest;
import com.back.auth.model.dto.request.RegisterRequest;
import com.back.auth.model.dto.response.AuthResult;
import com.back.auth.service.IAuthService;
import com.back.common.model.dto.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController{

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login (@Valid @RequestBody LoginRequest loginRequest){
        AuthResult result = authService.login(loginRequest);

        Cookie refreshCookie = new Cookie("refreshToken", result.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);

        result.setRefreshToken(null);

        ApiResponse<AuthResult> res = ApiResponse.<AuthResult>builder()
                .message("Đăng nhập thành công")
                .data(result)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(res);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterRequest registerRequest){
        return ResponseEntity.ok(ApiResponse.<AuthResult>builder()
                .message("Đăng ký thành công, vui lòng xác thực email để đăng nhập")
                        .data(authService.register(registerRequest))
                .status(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now()).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .message("Đăng xuất thành công")
                        .status(HttpStatus.NO_CONTENT.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(String token){
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .message("Xác thực email thành công vui lòng đăng nhập")
                        .status(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now())
                .build());
    }
}
