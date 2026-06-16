package com.back.report.service;

import com.back.comment.model.entity.Comment;
import com.back.comment.model.enums.CommentStatus;
import com.back.comment.repo.ICommentRepo;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.report.model.dto.request.ReviewReportRequestDTO;
import com.back.report.model.dto.response.AdminReportResponseDTO;
import com.back.report.model.entity.Report;
import com.back.report.model.enums.ReportResolutionAction;
import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import com.back.report.repo.IReportRepo;
import com.back.user.model.entity.User;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements IAdminReportService {

    private final IReportRepo reportRepo;
    private final IVideoRepository videoRepository;
    private final ICommentRepo commentRepo;
    private final IUserRepo userRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReportResponseDTO> listReports(String status, Pageable pageable) {
        Page<Report> reports = (status != null && !status.isBlank())
                ? reportRepo.findByStatusOrderByCreatedAtDesc(ReportStatus.valueOf(status), pageable)
                : reportRepo.findAllByOrderByCreatedAtDesc(pageable);
        return reports.map(this::toDto);
    }

    @Override
    @Transactional
    public AdminReportResponseDTO reviewReport(Long reportId, ReviewReportRequestDTO request, Long adminId) {
        Report report = reportRepo.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        ReportResolutionAction action = request.getAction() == null
                ? ReportResolutionAction.NONE
                : request.getAction();
        validateAction(report, request.getStatus(), action);
        applyResolutionAction(report, action, request.getNote());
        report.setStatus(request.getStatus());
        report.setReviewedBy(adminId);
        report.setReviewNote(normalizeNote(request.getNote()));
        report.setResolutionAction(action);
        report.setReviewedAt(LocalDateTime.now());
        return toDto(reportRepo.save(report));
    }

    private void validateAction(Report report, ReportStatus status, ReportResolutionAction action) {
        if (status == ReportStatus.RESOLVED && action == ReportResolutionAction.NONE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "action", "An action is required to resolve a report");
        }

        if (status != ReportStatus.RESOLVED && action != ReportResolutionAction.NONE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "action", "Actions can only be applied when resolving a report");
        }

        boolean supported = switch (action) {
            case NONE -> true;
            case MARK_VIDEO_NEED_REVIEW, REMOVE_VIDEO ->
                    report.getTargetType() == ReportTargetType.VIDEO
                            || report.getTargetType() == ReportTargetType.VIDEO_POST;
            case DELETE_COMMENT -> report.getTargetType() == ReportTargetType.COMMENT;
            case SUSPEND_USER, BAN_USER -> report.getTargetType() == ReportTargetType.USER;
        };

        if (!supported) {
            throw new AppException(ErrorCode.BAD_REQUEST, "action", "Action is not supported for this report target");
        }
    }

    private void applyResolutionAction(Report report, ReportResolutionAction action, String note) {
        switch (action) {
            case NONE -> {
            }
            case MARK_VIDEO_NEED_REVIEW -> markVideo(report, VideoModerationStatus.NEED_REVIEW, note);
            case REMOVE_VIDEO -> markVideo(report, VideoModerationStatus.REJECTED, note);
            case DELETE_COMMENT -> deleteComment(report);
            case SUSPEND_USER -> updateUserStatus(report, UserStatus.SUSPENDED, note);
            case BAN_USER -> updateUserStatus(report, UserStatus.BANNED, note);
        }
    }

    private void markVideo(Report report, VideoModerationStatus status, String note) {
        Video video = videoRepository.findById(report.getTargetId())
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        video.setModerationStatus(status);
        video.setModerationCheckedAt(LocalDateTime.now());
        video.setModerationReasonCode(report.getReasonCode());
        video.setModerationReasonMessage(normalizeNote(note));
        videoRepository.save(video);
    }

    private void deleteComment(Report report) {
        Comment comment = commentRepo.findById(report.getTargetId())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getStatus() == CommentStatus.DELETED) {
            return;
        }

        Video video = comment.getVideo();
        if (video != null) {
            long countToRemove = 1L + (comment.getParent() == null ? safe(comment.getReplyCount()) : 0L);
            video.setCommentCount(Math.max(0L, safe(video.getCommentCount()) - countToRemove));
            videoRepository.save(video);
        }

        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            parent.setReplyCount(Math.max(0L, safe(parent.getReplyCount()) - 1));
            commentRepo.save(parent);
        }

        comment.setStatus(CommentStatus.DELETED);
        comment.setDeletedAt(LocalDateTime.now());
        comment.setContent("");
        commentRepo.save(comment);
    }

    private void updateUserStatus(Report report, UserStatus status, String note) {
        User user = userRepo.findById(report.getTargetId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String reason = normalizeNote(note);
        if (reason == null) {
            reason = "Admin action from report #" + report.getId() + ": " + report.getReasonCode();
        }
        user.setStatus(status);
        user.setStatusReason(reason);
        userRepo.save(user);
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private AdminReportResponseDTO toDto(Report report) {
        return AdminReportResponseDTO.builder()
                .id(report.getId())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .reporterAvatarUrl(report.getReporter() != null ? report.getReporter().getAvatarUrl() : null)
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonId(report.getReason() != null ? report.getReason().getId() : null)
                .reasonCode(report.getReasonCode())
                .reasonLabel(report.getReason() != null ? report.getReason().getLabelEn() : null)
                .additionalNote(report.getAdditionalNote())
                .status(report.getStatus())
                .resolutionAction(report.getResolutionAction())
                .reviewedBy(report.getReviewedBy())
                .reviewNote(report.getReviewNote())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
