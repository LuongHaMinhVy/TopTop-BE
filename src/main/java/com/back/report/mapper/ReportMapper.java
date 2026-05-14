package com.back.report.mapper;

import com.back.report.model.dto.response.ReportPolicyResponseDTO;
import com.back.report.model.dto.response.ReportReasonResponseDTO;
import com.back.report.model.dto.response.ReportReasonTreeResponseDTO;
import com.back.report.model.dto.response.ReportResponseDTO;
import com.back.report.model.entity.Report;
import com.back.report.model.entity.ReportReason;
import com.back.report.repo.IReportReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final IReportReasonRepository reasonRepository;

    public ReportReasonResponseDTO toReasonResponseDTO(ReportReason reason) {
        if (reason == null) return null;
        
        boolean hasChildren = reasonRepository.existsByParentIdAndActiveTrue(reason.getId());

        String lang = LocaleContextHolder.getLocale().getLanguage();
        String label = "vi".equals(lang) ? reason.getLabelVi() : reason.getLabelEn();
        String description = "vi".equals(lang) ? reason.getDescriptionVi() : reason.getDescriptionEn();

        return ReportReasonResponseDTO.builder()
                .id(reason.getId())
                .code(reason.getCode())
                .label(label)
                .description(description)
                .hasChildren(hasChildren)
                .sortOrder(reason.getSortOrder())
                .build();
    }

    public ReportReasonTreeResponseDTO toReasonTreeResponseDTO(ReportReason reason) {
        if (reason == null) return null;

        List<ReportReason> children = reasonRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(reason.getId());
        List<ReportReasonTreeResponseDTO> childrenDTOs = children.stream()
                .map(this::toReasonTreeResponseDTO)
                .collect(Collectors.toList());

        String lang = LocaleContextHolder.getLocale().getLanguage();
        String label = "vi".equals(lang) ? reason.getLabelVi() : reason.getLabelEn();
        String description = "vi".equals(lang) ? reason.getDescriptionVi() : reason.getDescriptionEn();
        String policyText = "vi".equals(lang) ? reason.getPolicyTextVi() : reason.getPolicyTextEn();

        List<String> bullets = null;
        if (policyText != null && !policyText.isBlank()) {
            bullets = Arrays.asList(policyText.split("\\|"));
        }

        return ReportReasonTreeResponseDTO.builder()
                .id(reason.getId())
                .code(reason.getCode())
                .label(label)
                .description(description)
                .policyTitle(label)
                .policyBullets(bullets)
                .hasChildren(!children.isEmpty())
                .children(childrenDTOs)
                .build();
    }

    public ReportPolicyResponseDTO toPolicyResponseDTO(ReportReason reason) {
        if (reason == null) return null;

        String lang = LocaleContextHolder.getLocale().getLanguage();
        String label = "vi".equals(lang) ? reason.getLabelVi() : reason.getLabelEn();
        String description = "vi".equals(lang) ? reason.getDescriptionVi() : reason.getDescriptionEn();
        String policyText = "vi".equals(lang) ? reason.getPolicyTextVi() : reason.getPolicyTextEn();

        List<String> bullets = null;
        if (policyText != null && !policyText.isBlank()) {
            bullets = Arrays.asList(policyText.split("\\|"));
        }

        return ReportPolicyResponseDTO.builder()
                .reasonId(reason.getId())
                .code(reason.getCode())
                .title(label)
                .description(description)
                .bullets(bullets)
                .build();
    }

    public ReportResponseDTO toResponseDTO(Report report) {
        if (report == null) return null;

        String lang = LocaleContextHolder.getLocale().getLanguage();
        String label = "vi".equals(lang) ? report.getReason().getLabelVi() : report.getReason().getLabelEn();

        return ReportResponseDTO.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonId(report.getReason().getId())
                .reasonCode(report.getReasonCode())
                .reasonLabel(label)
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
