package com.back.report.model.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ReportPolicyResponseDTO {
    private Long reasonId;
    private String code;
    private String title;
    private String description;
    private List<String> bullets;
}
