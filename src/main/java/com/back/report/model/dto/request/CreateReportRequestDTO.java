package com.back.report.model.dto.request;

import com.back.report.model.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequestDTO {

    @NotNull(message = "report.targetType.required")
    private ReportTargetType targetType;

    @NotNull(message = "report.targetId.required")
    private Long targetId;

    @NotNull(message = "report.reasonId.required")
    private Long reasonId;

    @Size(max = 1000, message = "report.additionalNote.max")
    private String additionalNote;
}
