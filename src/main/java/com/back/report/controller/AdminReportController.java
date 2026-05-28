package com.back.report.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.report.model.dto.request.ReviewReportRequestDTO;
import com.back.report.model.dto.response.AdminReportResponseDTO;
import com.back.report.service.IAdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final IAdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminReportResponseDTO>>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminReportResponseDTO> data = adminReportService.listReports(status, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<AdminReportResponseDTO>>builder()
                .message(Translator.toLocale("admin.report.list.success", "Reports loaded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{reportId}/review")
    public ResponseEntity<ApiResponse<AdminReportResponseDTO>> reviewReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReviewReportRequestDTO request) {

        Long adminId = getCurrentAdminId();
        AdminReportResponseDTO data = adminReportService.reviewReport(reportId, request, adminId);

        return ResponseEntity.ok(ApiResponse.<AdminReportResponseDTO>builder()
                .message(Translator.toLocale("admin.report.review.success", "Report reviewed successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        // Admin ID resolution delegated to service layer if needed
        return null;
    }
}
