package com.back.report.service;

import com.back.report.model.dto.request.ReviewReportRequestDTO;
import com.back.report.model.dto.response.AdminReportResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminReportService {
    Page<AdminReportResponseDTO> listReports(String status, Pageable pageable);
    AdminReportResponseDTO reviewReport(Long reportId, ReviewReportRequestDTO request, Long adminId);
}
