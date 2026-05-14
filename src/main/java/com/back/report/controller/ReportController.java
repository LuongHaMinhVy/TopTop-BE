package com.back.report.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.common.utils.redis.RateLimit;
import com.back.report.model.dto.request.CreateReportRequestDTO;
import com.back.report.model.dto.response.ReportPolicyResponseDTO;
import com.back.report.model.dto.response.ReportReasonResponseDTO;
import com.back.report.model.dto.response.ReportReasonTreeResponseDTO;
import com.back.report.model.dto.response.ReportResponseDTO;
import com.back.report.service.IReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final IReportService reportService;

    @GetMapping("/reasons/tree")
    public ResponseEntity<ApiResponse<List<ReportReasonTreeResponseDTO>>> getReasonTree() {
        var data = reportService.getReportReasonTree();
        return ResponseEntity.ok(ApiResponse.<List<ReportReasonTreeResponseDTO>>builder()
            .message(Translator.toLocale("report.reason.tree.success", "Lấy danh sách lý do báo cáo thành công"))
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }

    @GetMapping("/reasons")
    public ResponseEntity<ApiResponse<List<ReportReasonResponseDTO>>> getRootReasons() {
        var data = reportService.getRootReasons();
        return ResponseEntity.ok(ApiResponse.<List<ReportReasonResponseDTO>>builder()
            .message(Translator.toLocale("report.reason.root.success", "Lấy danh sách nhóm báo cáo thành công"))
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }

    @GetMapping("/reasons/{parentId}/children")
    public ResponseEntity<ApiResponse<List<ReportReasonResponseDTO>>> getChildReasons(@PathVariable Long parentId) {
        var data = reportService.getChildReasons(parentId);
        return ResponseEntity.ok(ApiResponse.<List<ReportReasonResponseDTO>>builder()
            .message(Translator.toLocale("report.reason.child.success", "Lấy danh sách lý do thành công"))
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }

    @GetMapping("/reasons/{reasonId}/policy")
    public ResponseEntity<ApiResponse<ReportPolicyResponseDTO>> getReasonPolicy(@PathVariable Long reasonId) {
        var data = reportService.getReasonPolicy(reasonId);
        return ResponseEntity.ok(ApiResponse.<ReportPolicyResponseDTO>builder()
            .message(Translator.toLocale("report.reason.policy.success", "Lấy chính sách báo cáo thành công"))
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }

    @PostMapping
    @RateLimit(limit = 20, durationInSeconds = 600)
    public ResponseEntity<ApiResponse<ReportResponseDTO>> createReport(@Valid @RequestBody CreateReportRequestDTO request) {
        var data = reportService.createReport(request);
        return ResponseEntity.ok(ApiResponse.<ReportResponseDTO>builder()
            .message(Translator.toLocale("report.create.success", "Gửi báo cáo thành công"))
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }
}
