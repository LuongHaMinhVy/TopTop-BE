package com.back.report.model.dto.response;

import com.back.report.model.enums.ReportResolutionAction;
import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReportResponseDTO {
    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private String reporterAvatarUrl;
    private ReportTargetType targetType;
    private Long targetId;
    private Long reasonId;
    private String reasonCode;
    private String reasonLabel;
    private String additionalNote;
    private ReportStatus status;
    private ReportResolutionAction resolutionAction;
    private Long reviewedBy;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
