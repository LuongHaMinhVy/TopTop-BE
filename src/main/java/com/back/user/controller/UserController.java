package com.back.user.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.user.model.dto.request.AccountStatusActionRequestDTO;
import com.back.user.model.dto.request.AccountStatusConfirmRequestDTO;
import com.back.user.model.dto.request.AccountStatusOtpRequestDTO;
import com.back.user.model.dto.request.ChangePasswordRequestDTO;
import com.back.user.model.dto.request.ContentFilterTagRequestDTO;
import com.back.user.model.dto.request.UpdatePrivacySettingsRequestDTO;
import com.back.user.model.dto.response.ContentFilterTagResponseDTO;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.service.IUserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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

    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfo>> updateProfile(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.back.user.model.dto.request.UpdateProfileRequestDTO request) {
        UserInfo userInfo = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.<UserInfo>builder()
                .message("Profile updated successfully")
                .data(userInfo)
                .status(HttpStatus.OK.value())
                .build());
    }

    @PatchMapping("/settings/privacy")
    public ResponseEntity<ApiResponse<UserInfo>> updatePrivacySettings(
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdatePrivacySettingsRequestDTO request) {
        UserInfo userInfo = userService.updatePrivacySettings(request);
        return ResponseEntity.ok(ApiResponse.<UserInfo>builder()
                .message(Translator.toLocale("user.settings.privacy.success", "Privacy settings updated successfully"))
                .data(userInfo)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/settings/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequestDTO request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("user.settings.password.success", "Password updated successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/settings/account-status")
    public ResponseEntity<ApiResponse<Void>> updateAccountStatus(
            @Valid @org.springframework.web.bind.annotation.RequestBody AccountStatusActionRequestDTO request) {
        userService.updateAccountStatus(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("user.settings.account_status.success", "Account status updated successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/settings/account-status/otp")
    public ResponseEntity<ApiResponse<Void>> sendAccountStatusOtp(
            @Valid @org.springframework.web.bind.annotation.RequestBody AccountStatusOtpRequestDTO request) {
        userService.sendAccountStatusOtp(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("user.settings.account_status.otp.success", "OTP sent successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/settings/account-status/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAccountStatus(
            @Valid @org.springframework.web.bind.annotation.RequestBody AccountStatusConfirmRequestDTO request) {
        userService.confirmAccountStatus(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("user.settings.account_status.success", "Account status updated successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/settings/content-filters")
    public ResponseEntity<ApiResponse<List<ContentFilterTagResponseDTO>>> getContentFilterTags() {
        return ResponseEntity.ok(ApiResponse.<List<ContentFilterTagResponseDTO>>builder()
                .message(Translator.toLocale("user.settings.content_filters.success", "Content filters loaded successfully"))
                .data(userService.getContentFilterTags())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/settings/content-filters")
    public ResponseEntity<ApiResponse<ContentFilterTagResponseDTO>> addContentFilterTag(
            @Valid @org.springframework.web.bind.annotation.RequestBody ContentFilterTagRequestDTO request) {
        ContentFilterTagResponseDTO data = userService.addContentFilterTag(request);
        return ResponseEntity.ok(ApiResponse.<ContentFilterTagResponseDTO>builder()
                .message(Translator.toLocale("user.settings.content_filter.add.success", "Content filter added successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/settings/content-filters/{tag}")
    public ResponseEntity<ApiResponse<Void>> deleteContentFilterTag(@PathVariable String tag) {
        userService.deleteContentFilterTag(tag);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("user.settings.content_filter.delete.success", "Content filter removed successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @org.springframework.web.bind.annotation.PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        String avatarUrl = userService.uploadAvatar(file);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Avatar uploaded successfully")
                .data(avatarUrl)
                .status(HttpStatus.OK.value())
                .build());
    }
}
