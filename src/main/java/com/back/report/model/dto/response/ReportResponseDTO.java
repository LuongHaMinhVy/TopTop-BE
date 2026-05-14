package com.back.report.model.dto.response;

import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReportResponseDTO {
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private Long reasonId;
    private String reasonCode;
    private String reasonLabel;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
