package com.back.moderation.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.model.dto.request.ReviewVideoModerationRequestDTO;
import com.back.moderation.model.dto.response.*;
import com.back.moderation.model.entity.ModerationAuditLog;
import com.back.moderation.model.entity.VideoModerationFrame;
import com.back.moderation.model.entity.VideoModerationResult;
import com.back.moderation.model.enums.*;
import com.back.moderation.repository.IModerationAuditLogRepository;
import com.back.moderation.repository.IVideoModerationFrameRepository;
import com.back.moderation.repository.IVideoModerationResultRepository;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoModerationServiceImpl implements IVideoModerationService {

    private static final Set<String> ALLOWED_QUALITY_ISSUES = Set.of(
            "WATERMARK",
            "QR_CODE",
            "LOW_QUALITY"
    );

    private final IVideoRepository videoRepository;
    private final IVideoModerationResultRepository moderationResultRepository;
    private final IVideoModerationFrameRepository moderationFrameRepository;
    private final IModerationAuditLogRepository auditLogRepository;
    private final GeminiModerationProvider moderationProvider;
    private final IMusicCopyrightService musicCopyrightService;
    private final FrameExtractionService frameExtractionService;

    @Value("${moderation.auto-approve-threshold:0.35}")
    private double autoApproveThreshold;

    @Value("${moderation.auto-reject-threshold:0.85}")
    private double autoRejectThreshold;

    @Value("${moderation.frame-sampling.max-frames:8}")
    private int maxFrames;

    @Override
    @Async
    @Transactional
    public void runModeration(Long videoId) {
        runModeration(videoId, true, true);
    }

    @Override
    @Async
    @Transactional
    public void runModeration(Long videoId, boolean musicCopyrightCheckEnabled, boolean contentModerationCheckEnabled) {
        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || video.isDeleted()) {
                log.warn("Moderation skipped: video {} not found or deleted", videoId);
                return;
            }

            log.info("Starting moderation for video {}", videoId);
            video.setModerationStatus(VideoModerationStatus.PENDING);
            videoRepository.save(video);

            // --- Music copyright moderation ---
            MusicCopyrightStatus previousMusicStatus = video.getMusicCopyrightStatus();
            if (musicCopyrightCheckEnabled) {
                MusicCopyrightResult musicResult = musicCopyrightService.check(video);
                video.setMusicCopyrightStatus(musicResult.status());
                video.setMusicCopyrightCheckedAt(LocalDateTime.now());
                video.setMusicCopyrightReasonCode(musicResult.reasonCode());
                video.setMusicCopyrightReasonMessage(musicResult.reasonMessage());
                videoRepository.save(video);

                ModerationAuditAction musicAuditAction = switch (musicResult.status()) {
                    case APPROVED -> ModerationAuditAction.AUTO_APPROVE;
                    case REJECTED -> ModerationAuditAction.AUTO_REJECT;
                    case NEED_REVIEW, PENDING -> ModerationAuditAction.MARK_NEED_REVIEW;
                };
                saveAuditLog("VIDEO_MUSIC", videoId, null, ModerationActorType.SYSTEM,
                        previousMusicStatus != null ? previousMusicStatus.name() : null,
                        musicResult.status().name(), musicAuditAction,
                        musicResult.reasonCode(), musicResult.reasonMessage());
            } else {
                video.setMusicCopyrightStatus(MusicCopyrightStatus.APPROVED);
                video.setMusicCopyrightCheckedAt(LocalDateTime.now());
                video.setMusicCopyrightReasonCode("MUSIC_COPYRIGHT_CHECK_SKIPPED");
                video.setMusicCopyrightReasonMessage("Người dùng đã tắt kiểm tra bản quyền nhạc khi đăng video.");
                videoRepository.save(video);
                saveAuditLog("VIDEO_MUSIC", videoId, null, ModerationActorType.SYSTEM,
                        previousMusicStatus != null ? previousMusicStatus.name() : null,
                        MusicCopyrightStatus.APPROVED.name(), ModerationAuditAction.AUTO_APPROVE,
                        "MUSIC_COPYRIGHT_CHECK_SKIPPED",
                        "Người dùng đã tắt kiểm tra bản quyền nhạc khi đăng video.");
            }

            // --- Text moderation ---
            ModerationProviderResult textResult;
            if (contentModerationCheckEnabled) {
                List<String> hashtags = extractHashtags(video.getDescription());
                TextModerationInput textInput = new TextModerationInput(video.getDescription(), hashtags);
                textResult = moderationProvider.moderateText(textInput);
            } else {
                textResult = new ModerationProviderResult(
                        0.0,
                        List.of(),
                        "CONTENT_MODERATION_CHECK_SKIPPED",
                        "Người dùng đã tắt kiểm tra nội dung khi đăng video.");
            }
            double textRisk = textResult.riskScore();

            // --- Frame extraction & image moderation (FFmpeg + Gemini Vision) ---
            List<VideoModerationFrame> frames = contentModerationCheckEnabled
                    ? extractAndAnalyseFrames(video)
                    : List.of();
            double imageRisk = frames.stream()
                    .mapToDouble(f -> f.getRiskScore() != null ? f.getRiskScore() : 0.0)
                    .max().orElse(0.0);

            // Quality issues are warnings only. They are persisted for the UI but never
            // contribute to moderation rejection or public visibility decisions.
            Set<String> allQualityIssues = new LinkedHashSet<>();
            frames.forEach(f -> {
                allQualityIssues.addAll(parseQualityIssues(f.getQualityIssuesJson()));
            });
            String qualityIssuesJson = allQualityIssues.isEmpty() ? null
                    : String.join(",", allQualityIssues);

            // --- Aggregate ---
            double riskScore = Math.max(textRisk, imageRisk);

            // --- Decision ---
            ModerationDecision decision;
            VideoModerationStatus newStatus;
            String reasonCode = textResult.reasonCode();
            String reasonMessage = textResult.reasonMessage();
            ModerationAuditAction auditAction;

            if (riskScore >= autoRejectThreshold) {
                decision = ModerationDecision.REJECT;
                newStatus = VideoModerationStatus.REJECTED;
                auditAction = ModerationAuditAction.AUTO_REJECT;
                if (reasonCode == null) reasonCode = "HIGH_RISK_SCORE";
                if (reasonMessage == null) reasonMessage = "Nội dung bị từ chối tự động do điểm rủi ro cao.";
            } else if (riskScore >= autoApproveThreshold) {
                decision = ModerationDecision.NEED_REVIEW;
                newStatus = VideoModerationStatus.NEED_REVIEW;
                auditAction = ModerationAuditAction.MARK_NEED_REVIEW;
                if (reasonCode == null) reasonCode = "NEEDS_HUMAN_REVIEW";
                if (reasonMessage == null) reasonMessage = "Video đang chờ quản trị viên kiểm tra.";
            } else {
                decision = ModerationDecision.APPROVE;
                newStatus = VideoModerationStatus.APPROVED;
                auditAction = ModerationAuditAction.AUTO_APPROVE;
            }

            // --- Save result ---
            VideoModerationResult result = VideoModerationResult.builder()
                    .video(video)
                    .provider(ModerationProviderType.LOCAL_RULES)
                    .providerVersion("1.0")
                    .status(newStatus)
                    .finalDecision(decision)
                    .riskScore(riskScore)
                    .textRiskScore(textRisk)
                    .imageRiskScore(imageRisk)
                    .sampledFrameCount(frames.size())
                    .categoriesJson(String.join(",", textResult.categories()))
                    .qualityIssuesJson(qualityIssuesJson)
                    .reasonCode(reasonCode)
                    .reasonMessage(reasonMessage)
                    .checkedAt(LocalDateTime.now())
                    .build();
            result = moderationResultRepository.save(result);

            // Save frames linked to result
            for (VideoModerationFrame frame : frames) {
                frame.setModerationResult(result);
            }
            moderationFrameRepository.saveAll(frames);

            // --- Update video ---
            String previousStatus = video.getModerationStatus().name();
            video.setModerationStatus(newStatus);
            video.setModerationCheckedAt(LocalDateTime.now());
            video.setModerationReasonCode(reasonCode);
            video.setModerationReasonMessage(reasonMessage);
            video.setQualityIssuesJson(qualityIssuesJson);
            video.setQualityIssueMessage(buildQualityIssueMessage(new ArrayList<>(allQualityIssues)));
            videoRepository.save(video);

            // --- Audit log ---
            saveAuditLog("VIDEO", videoId, null, ModerationActorType.SYSTEM,
                    previousStatus, newStatus.name(), auditAction, reasonCode, reasonMessage);

            log.info("Moderation done for video {}: {} (risk={})", videoId, newStatus, riskScore);
        } catch (Exception e) {
            log.error("Moderation failed for video {}: {}", videoId, e.getMessage(), e);
            videoRepository.findById(videoId).ifPresent(v -> {
                v.setModerationStatus(VideoModerationStatus.NEED_REVIEW);
                v.setModerationReasonCode("MODERATION_ERROR");
                v.setModerationReasonMessage("Lỗi trong quá trình kiểm duyệt tự động. Cần duyệt thủ công.");
                if (v.getMusicCopyrightStatus() == MusicCopyrightStatus.PENDING) {
                    v.setMusicCopyrightStatus(MusicCopyrightStatus.NEED_REVIEW);
                    v.setMusicCopyrightCheckedAt(LocalDateTime.now());
                    v.setMusicCopyrightReasonCode("MUSIC_COPYRIGHT_CHECK_ERROR");
                    v.setMusicCopyrightReasonMessage("Lỗi kiểm tra bản quyền nhạc. Cần duyệt thủ công.");
                }
                videoRepository.save(v);
            });
        }
    }

    /**
     * Extracts real JPEG frames from the video using FFmpeg, then sends each frame
     * to Gemini Vision for combined safety + quality analysis.
     * Falls back gracefully to empty frames on any extraction failure.
     */
    private List<VideoModerationFrame> extractAndAnalyseFrames(Video video) {
        String videoUrl = video.getFileUrl();
        if (videoUrl == null || videoUrl.isBlank()) {
            log.warn("Video {} has no fileUrl, skipping frame analysis", video.getId());
            return List.of();
        }

        List<byte[]> frameBytes = frameExtractionService.extractFrames(videoUrl);
        if (frameBytes.isEmpty()) {
            log.warn("No frames extracted for video {}", video.getId());
            return List.of();
        }

        List<VideoModerationFrame> frames = new ArrayList<>();
        for (int i = 0; i < frameBytes.size(); i++) {
            byte[] bytes = frameBytes.get(i);
            VideoFrameAnalysisResult analysis = moderationProvider.analyzeVideoFrame(bytes);

            String categoriesJson = analysis.categories().isEmpty() ? null
                    : String.join(",", analysis.categories());
            List<String> qualityIssues = normalizeQualityIssues(analysis.qualityIssues());
            String qualityIssuesJson = qualityIssues.isEmpty() ? null : String.join(",", qualityIssues);

            frames.add(VideoModerationFrame.builder()
                    .video(video)
                    .frameIndex(i)
                    .timestampMs((long) i * 4_000) // approximate: 1 frame every 4 s
                    .riskScore(analysis.riskScore())
                    .categoriesJson(categoriesJson)
                    .qualityIssuesJson(qualityIssuesJson)
                    .build());

            log.debug("Frame {}/{} for video {}: risk={}, quality={}, safety={}",
                    i + 1, frameBytes.size(), video.getId(),
                    analysis.riskScore(), analysis.qualityIssues(), analysis.categories());
        }
        return frames;
    }

    @Override
    public VideoModerationSummaryResponseDTO getModerationStatus(Long videoId, Long requesterId, boolean isAdmin) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        if (!isAdmin && !video.getUser().getId().equals(requesterId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        VideoModerationResult result = moderationResultRepository
                .findTopByVideoIdOrderByCreatedAtDesc(videoId).orElse(null);

        List<String> qualityIssues = parseQualityIssues(firstNonBlank(
                video.getQualityIssuesJson(),
                result != null ? result.getQualityIssuesJson() : null));
        String qualityIssueMessage = firstNonBlank(video.getQualityIssueMessage(), buildQualityIssueMessage(qualityIssues));

        return VideoModerationSummaryResponseDTO.builder()
                .videoId(videoId)
                .moderationStatus(video.getModerationStatus().name())
                .riskScore(result != null ? result.getRiskScore() : null)
                .reasonCode(video.getModerationReasonCode())
                .reasonMessage(video.getModerationReasonMessage())
                .checkedAt(video.getModerationCheckedAt())
                .musicCopyrightStatus(video.getMusicCopyrightStatus() != null ? video.getMusicCopyrightStatus().name() : null)
                .musicCopyrightReasonCode(video.getMusicCopyrightReasonCode())
                .musicCopyrightReasonMessage(video.getMusicCopyrightReasonMessage())
                .musicCopyrightCheckedAt(video.getMusicCopyrightCheckedAt())
                .qualityIssues(qualityIssues)
                .qualityIssueMessage(qualityIssueMessage)
                .build();
    }

    @Override
    public Page<ModerationQueueItemResponseDTO> getAdminQueue(VideoModerationStatus status, Pageable pageable) {
        Page<VideoModerationResult> results = status != null
                ? moderationResultRepository.findByVideoModerationStatus(status, pageable)
                : moderationResultRepository.findAll(pageable);

        return results.map(r -> {
            Video v = r.getVideo();
            return ModerationQueueItemResponseDTO.builder()
                    .videoId(v.getId())
                    .caption(v.getDescription())
                    .coverUrl(v.getThumbnailUrl())
                    .authorUsername(v.getUser().getUsername())
                    .authorAvatarUrl(v.getUser().getAvatarUrl())
                    .moderationStatus(v.getModerationStatus().name())
                    .riskScore(r.getRiskScore())
                    .categories(r.getCategoriesJson() != null
                            ? List.of(r.getCategoriesJson().split(","))
                            : List.of())
                    .createdAt(v.getCreatedAt())
                    .checkedAt(r.getCheckedAt())
                    .build();
        });
    }

    @Override
    public VideoModerationDetailResponseDTO getAdminDetail(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        VideoModerationResult result = moderationResultRepository
                .findTopByVideoIdOrderByCreatedAtDesc(videoId).orElse(null);

        List<VideoModerationFrame> frames = moderationFrameRepository.findByVideoIdOrderByFrameIndex(videoId);
        List<ModerationAuditLog> auditLogs = auditLogRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("VIDEO", videoId);
        List<String> qualityIssues = parseQualityIssues(firstNonBlank(
                video.getQualityIssuesJson(),
                result != null ? result.getQualityIssuesJson() : null));

        return VideoModerationDetailResponseDTO.builder()
                .videoId(videoId)
                .videoPreviewUrl(video.getFileUrl())
                .coverUrl(video.getThumbnailUrl())
                .caption(video.getDescription())
                .authorUsername(video.getUser().getUsername())
                .authorAvatarUrl(video.getUser().getAvatarUrl())
                .moderationStatus(video.getModerationStatus().name())
                .riskScore(result != null ? result.getRiskScore() : null)
                .textRiskScore(result != null ? result.getTextRiskScore() : null)
                .imageRiskScore(result != null ? result.getImageRiskScore() : null)
                .categories(result != null && result.getCategoriesJson() != null
                        ? List.of(result.getCategoriesJson().split(","))
                        : List.of())
                .qualityIssues(qualityIssues)
                .qualityIssueMessage(firstNonBlank(video.getQualityIssueMessage(), buildQualityIssueMessage(qualityIssues)))
                .frames(frames.stream().map(f -> VideoModerationFrameResponseDTO.builder()
                        .frameIndex(f.getFrameIndex())
                        .timestampMs(f.getTimestampMs())
                        .riskScore(f.getRiskScore())
                        .categoriesJson(f.getCategoriesJson())
                        .qualityIssuesJson(f.getQualityIssuesJson())
                        .build()).toList())
                .auditLogs(auditLogs.stream().map(log -> ModerationAuditLogResponseDTO.builder()
                        .action(log.getAction().name())
                        .actorType(log.getActorType().name())
                        .actorUserId(log.getActorUserId())
                        .previousStatus(log.getPreviousStatus())
                        .newStatus(log.getNewStatus())
                        .reasonCode(log.getReasonCode())
                        .reasonMessage(log.getReasonMessage())
                        .createdAt(log.getCreatedAt())
                        .build()).toList())
                .checkedAt(video.getModerationCheckedAt())
                .build();
    }

    @Override
    @Transactional
    public VideoModerationSummaryResponseDTO reviewVideo(Long videoId, ReviewVideoModerationRequestDTO request, Long adminUserId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));

        String previousStatus = video.getModerationStatus().name();

        VideoModerationStatus newStatus = switch (request.getDecision()) {
            case APPROVE -> VideoModerationStatus.APPROVED;
            case REJECT -> VideoModerationStatus.REJECTED;
            case NEED_REVIEW -> VideoModerationStatus.NEED_REVIEW;
        };

        ModerationAuditAction auditAction = switch (request.getDecision()) {
            case APPROVE -> ModerationAuditAction.MANUAL_APPROVE;
            case REJECT -> ModerationAuditAction.MANUAL_REJECT;
            case NEED_REVIEW -> ModerationAuditAction.MARK_NEED_REVIEW;
        };

        video.setModerationStatus(newStatus);
        video.setModerationCheckedAt(LocalDateTime.now());
        video.setModerationReasonCode(request.getReasonCode());
        video.setModerationReasonMessage(request.getReasonMessage());
        videoRepository.save(video);

        saveAuditLog("VIDEO", videoId, adminUserId, ModerationActorType.ADMIN,
                previousStatus, newStatus.name(), auditAction,
                request.getReasonCode(), request.getReasonMessage());

        VideoModerationResult latestResult = moderationResultRepository
                .findTopByVideoIdOrderByCreatedAtDesc(videoId).orElse(null);
        List<String> qualityIssues = parseQualityIssues(firstNonBlank(
                video.getQualityIssuesJson(),
                latestResult != null ? latestResult.getQualityIssuesJson() : null));

        return VideoModerationSummaryResponseDTO.builder()
                .videoId(videoId)
                .moderationStatus(newStatus.name())
                .reasonCode(request.getReasonCode())
                .reasonMessage(request.getReasonMessage())
                .checkedAt(video.getModerationCheckedAt())
                .musicCopyrightStatus(video.getMusicCopyrightStatus() != null ? video.getMusicCopyrightStatus().name() : null)
                .musicCopyrightReasonCode(video.getMusicCopyrightReasonCode())
                .musicCopyrightReasonMessage(video.getMusicCopyrightReasonMessage())
                .musicCopyrightCheckedAt(video.getMusicCopyrightCheckedAt())
                .qualityIssues(qualityIssues)
                .qualityIssueMessage(firstNonBlank(video.getQualityIssueMessage(), buildQualityIssueMessage(qualityIssues)))
                .build();
    }

    private void saveAuditLog(String targetType, Long targetId, Long actorUserId,
                               ModerationActorType actorType, String previousStatus, String newStatus,
                               ModerationAuditAction action, String reasonCode, String reasonMessage) {
        auditLogRepository.save(ModerationAuditLog.builder()
                .targetType(targetType)
                .targetId(targetId)
                .actorUserId(actorUserId)
                .actorType(actorType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .action(action)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .build());
    }

    private List<String> extractHashtags(String text) {
        if (text == null) return List.of();
        List<String> tags = new ArrayList<>();
        Matcher m = Pattern.compile("#(\\w+)").matcher(text);
        while (m.find()) tags.add(m.group(1));
        return tags;
    }

    /** Parses a comma-separated quality issues string into a list. Returns empty list when null. */
    private List<String> parseQualityIssues(String qualityIssuesJson) {
        if (qualityIssuesJson == null || qualityIssuesJson.isBlank()) return List.of();
        return Arrays.stream(qualityIssuesJson.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(ALLOWED_QUALITY_ISSUES::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeQualityIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) return List.of();
        return issues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(ALLOWED_QUALITY_ISSUES::contains)
                .distinct()
                .toList();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    /** Builds a Vietnamese warning message summarising detected quality issues. */
    private String buildQualityIssueMessage(List<String> issues) {
        if (issues == null || issues.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        if (issues.contains("WATERMARK"))
            parts.add("watermark hoặc logo của nền tảng khác");
        if (issues.contains("QR_CODE"))
            parts.add("mã QR code");
        if (issues.contains("LOW_QUALITY"))
            parts.add("chất lượng hình ảnh thấp");
        if (parts.isEmpty()) return null;
        return "Phát hiện " + String.join(", ", parts) + " trong video.";
    }
}
