package com.back.report.model.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReportReasonResponseDTO {
    private Long id;
    private String code;
    private String label;
    private String description;
    private Boolean hasChildren;
    private Integer sortOrder;
}
