package com.back.user.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.user.model.dto.request.AdminUpdateUserStatusRequestDTO;
import com.back.user.model.dto.response.AdminUserResponseDTO;
import com.back.user.model.enums.UserStatus;
import com.back.user.service.IAdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IAdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponseDTO>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminUserResponseDTO> data = adminUserService.listUsers(keyword, status, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<AdminUserResponseDTO>>builder()
                .message(Translator.toLocale("admin.user.list.success", "Users loaded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponseDTO>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequestDTO request) {

        AdminUserResponseDTO data = adminUserService.updateUserStatus(userId, request);

        return ResponseEntity.ok(ApiResponse.<AdminUserResponseDTO>builder()
                .message(Translator.toLocale("admin.user.status.success", "User status updated successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
