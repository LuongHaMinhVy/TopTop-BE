package com.back.report.service;

import com.back.comment.repo.ICommentRepo;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import com.back.report.mapper.ReportMapper;
import com.back.report.model.dto.request.CreateReportRequestDTO;
import com.back.report.model.dto.response.ReportPolicyResponseDTO;
import com.back.report.model.dto.response.ReportReasonResponseDTO;
import com.back.report.model.dto.response.ReportReasonTreeResponseDTO;
import com.back.report.model.dto.response.ReportResponseDTO;
import com.back.report.model.entity.Report;
import com.back.report.model.entity.ReportReason;
import com.back.report.repo.IReportReasonRepository;
import com.back.report.repo.IReportRepository;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private final IReportReasonRepository reasonRepository;
    private final IReportRepository reportRepository;
    private final ReportMapper reportMapper;
    
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final ICommentRepo commentRepository;

    @Override
    public List<ReportReasonTreeResponseDTO> getReportReasonTree() {
        return reasonRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(reportMapper::toReasonTreeResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportReasonResponseDTO> getRootReasons() {
        return reasonRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(reportMapper::toReasonResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportReasonResponseDTO> getChildReasons(Long parentId) {
        return reasonRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId)
                .stream()
                .map(reportMapper::toReasonResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReportPolicyResponseDTO getReasonPolicy(Long reasonId) {
        ReportReason reason = reasonRepository.findByIdAndActiveTrue(reasonId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_REASON_NOT_FOUND));
        return reportMapper.toPolicyResponseDTO(reason);
    }

    @Override
    @Transactional
    public ReportResponseDTO createReport(CreateReportRequestDTO requestDTO) {
        String currentEmailOrUsername = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        User currentUser = userRepo.findByEmail(currentEmailOrUsername)
                .orElseGet(() -> userRepo.findByUsername(currentEmailOrUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));

        validateTargetExists(requestDTO.getTargetType(), requestDTO.getTargetId());

        ReportReason reason = reasonRepository.findByIdAndActiveTrue(requestDTO.getReasonId())
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_REASON_NOT_FOUND));

        if (reasonRepository.existsByParentIdAndActiveTrue(reason.getId())) {
            throw new AppException(ErrorCode.REPORT_REASON_MUST_BE_LEAF);
        }

        validateDuplicateReport(currentUser.getId(), requestDTO, reason.getId());

        String safeNote = requestDTO.getAdditionalNote() != null ? 
            requestDTO.getAdditionalNote().replaceAll("<.*?>", "") : null;

        Report report = Report.builder()
                .reporter(currentUser)
                .targetType(requestDTO.getTargetType())
                .targetId(requestDTO.getTargetId())
                .reason(reason)
                .reasonCode(reason.getCode())
                .additionalNote(safeNote)
                .status(ReportStatus.PENDING)
                .build();

        return reportMapper.toResponseDTO(reportRepository.save(report));
    }

    private void validateTargetExists(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case VIDEO:
                if (!videoRepository.existsById(targetId)) {
                    throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
                }
                break;
            case COMMENT:
                if (!commentRepository.existsById(targetId)) {
                    throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
                }
                break;
            case USER:
                if (!userRepo.existsById(targetId)) {
                    throw new AppException(ErrorCode.USER_NOT_FOUND);
                }
                break;
            default:
                break;
        }
    }

    private void validateDuplicateReport(Long reporterId, CreateReportRequestDTO requestDTO, Long reasonId) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndReasonId(
                reporterId, requestDTO.getTargetType(), requestDTO.getTargetId(), reasonId)) {
            throw new AppException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }
    }
}
