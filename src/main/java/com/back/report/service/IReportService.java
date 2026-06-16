package com.back.report.service;

import com.back.report.model.dto.request.CreateReportRequestDTO;
import com.back.report.model.dto.response.ReportPolicyResponseDTO;
import com.back.report.model.dto.response.ReportReasonResponseDTO;
import com.back.report.model.dto.response.ReportReasonTreeResponseDTO;
import com.back.report.model.dto.response.ReportResponseDTO;

import java.util.List;

public interface IReportService {

    List<ReportReasonTreeResponseDTO> getReportReasonTree();

    List<ReportReasonResponseDTO> getRootReasons();

    List<ReportReasonResponseDTO> getChildReasons(Long parentId);

    ReportPolicyResponseDTO getReasonPolicy(Long reasonId);

    ReportResponseDTO createReport(CreateReportRequestDTO requestDTO);
}
