package com.back.auth.controller;

import com.back.auth.model.dto.request.LoginRequest;
import com.back.auth.model.dto.request.RegisterRequest;
import com.back.auth.model.dto.request.ResetPasswordRequest;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.service.IAuthService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.common.utils.redis.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController{

    private final IAuthService authService;

    @PostMapping("/login")
    @RateLimit(limit = 10, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request){
        AuthResponse result = authService.login(loginRequest, response, request).getAuthResponse();

        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .message(Translator.toLocale("auth.login.success", "Login successful"))
                .data(result)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/register")
    @RateLimit(limit = 10, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest){
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Void>builder()
                .message(Translator.toLocale("auth.register.success", "Registration successful. Please verify your email to login"))
                .status(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now()).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .message(Translator.toLocale("auth.logout.success", "Logout successful"))
                        .status(HttpStatus.NO_CONTENT.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/reactivate")
    public ResponseEntity<ApiResponse<AuthResponse>> reactivate(HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .message(Translator.toLocale("auth.reactivate.success", "Account reactivated successfully"))
                .data(authService.reactivateAccount(request, response))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token){
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .message(Translator.toLocale("auth.verify_email.success", "Email verified successfully. Please login"))
                        .status(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/resend-verification")
    @RateLimit(limit = 3, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email){
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("auth.resend_verification.success", "Verification email resent successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/forgot-password")
    @RateLimit(limit = 3, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email){
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("auth.forgot_password.success", "Password reset email sent successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/reset-password")
    @RateLimit(limit = 10, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("auth.reset_password.success", "Password reset successfully. Please login"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .message(Translator.toLocale("auth.refresh.success", "Token refreshed successfully"))
                .data(authService.refreshToken(request, response))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/oauth2/onboard")
    public ResponseEntity<ApiResponse<AuthResponse>> onboardOAuth2(
            @Valid @RequestBody com.back.auth.model.dto.request.OAuth2OnboardRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthResponse result = authService.onboardOAuth2(request, servletRequest, servletResponse);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .message("Onboarding successful")
                .data(result)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
