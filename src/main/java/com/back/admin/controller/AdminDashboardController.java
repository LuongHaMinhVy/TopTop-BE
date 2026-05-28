package com.back.admin.controller;

import com.back.admin.model.dto.response.AdminDashboardStatsResponseDTO;
import com.back.admin.service.IAdminDashboardService;
import com.back.common.model.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IAdminDashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponseDTO>> getStats() {
        AdminDashboardStatsResponseDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.<AdminDashboardStatsResponseDTO>builder()
                .data(stats)
                .message("Dashboard stats retrieved successfully")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
