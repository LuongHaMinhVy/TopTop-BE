package com.back.report.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.report.model.dto.request.ReviewReportRequestDTO;
import com.back.report.model.dto.response.AdminReportResponseDTO;
import com.back.report.model.entity.Report;
import com.back.report.model.enums.ReportStatus;
import com.back.report.repo.IReportRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements IAdminReportService {

    private final IReportRepo reportRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReportResponseDTO> listReports(String status, Pageable pageable) {
        Page<Report> reports = (status != null && !status.isBlank())
                ? reportRepo.findByStatusOrderByCreatedAtDesc(ReportStatus.valueOf(status), pageable)
                : reportRepo.findAllByOrderByCreatedAtDesc(pageable);
        return reports.map(this::toDto);
    }

    @Override
    @Transactional
    public AdminReportResponseDTO reviewReport(Long reportId, ReviewReportRequestDTO request, Long adminId) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        report.setStatus(request.getStatus());
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());
        return toDto(reportRepo.save(report));
    }

    private AdminReportResponseDTO toDto(Report report) {
        return AdminReportResponseDTO.builder()
                .id(report.getId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .reporterAvatarUrl(report.getReporter() != null ? report.getReporter().getAvatarUrl() : null)
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonId(report.getReason() != null ? report.getReason().getId() : null)
                .reasonCode(report.getReasonCode())
                .reasonLabel(report.getReason() != null ? report.getReason().getLabelEn() : null)
                .additionalNote(report.getAdditionalNote())
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
