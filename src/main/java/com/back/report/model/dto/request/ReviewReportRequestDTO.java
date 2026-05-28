package com.back.report.model.dto.request;

import com.back.report.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewReportRequestDTO {
    @NotNull
    private ReportStatus status;

    private String note;
}
