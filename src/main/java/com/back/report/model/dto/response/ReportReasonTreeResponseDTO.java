package com.back.report.model.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ReportReasonTreeResponseDTO {
    private Long id;
    private String code;
    private String label;
    private String description;
    private String policyTitle;
    private List<String> policyBullets;
    private Boolean hasChildren;
    private List<ReportReasonTreeResponseDTO> children;
}
