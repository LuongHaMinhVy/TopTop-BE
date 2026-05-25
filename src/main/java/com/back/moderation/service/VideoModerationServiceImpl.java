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
import com.back.video.service.VideoDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoModerationServiceImpl implements IVideoModerationService {

    private final IVideoRepository videoRepository;
    private final IVideoModerationResultRepository moderationResultRepository;
    private final IVideoModerationFrameRepository moderationFrameRepository;
    private final IModerationAuditLogRepository auditLogRepository;
    private final GeminiModerationProvider moderationProvider;
    private final IMusicCopyrightService musicCopyrightService;
    private final VideoDeletionService videoDeletionService;

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
            MusicCopyrightResult musicResult = musicCopyrightService.check(video);
            MusicCopyrightStatus previousMusicStatus = video.getMusicCopyrightStatus();
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

            // --- Text moderation ---
            List<String> hashtags = extractHashtags(video.getDescription());
            TextModerationInput textInput = new TextModerationInput(video.getDescription(), hashtags);
            ModerationProviderResult textResult = moderationProvider.moderateText(textInput);
            double textRisk = textResult.riskScore();

            // --- Frame extraction & image moderation ---
            // With LOCAL_RULES provider: frame risk = 0 (no AI). Save timestamps only.
            List<VideoModerationFrame> frames = sampleFrames(video);
            double imageRisk = frames.stream()
                    .mapToDouble(f -> f.getRiskScore() != null ? f.getRiskScore() : 0.0)
                    .max().orElse(0.0);

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
            videoRepository.save(video);

            // --- Audit log ---
            saveAuditLog("VIDEO", videoId, null, ModerationActorType.SYSTEM,
                    previousStatus, newStatus.name(), auditAction, reasonCode, reasonMessage);

            if (newStatus == VideoModerationStatus.REJECTED
                    || musicResult.status() == MusicCopyrightStatus.REJECTED) {
                videoDeletionService.hardDelete(video);
                log.info("Rejected video {} was hard deleted after moderation", videoId);
                return;
            }

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

    private List<VideoModerationFrame> sampleFrames(Video video) {
        // Compute timestamps to sample (in ms)
        int durationSec = video.getDuration() != null ? video.getDuration() : 0;
        List<Long> timestamps = new ArrayList<>();
        if (durationSec > 0) {
            timestamps.add(1000L); // 1s
            timestamps.add((long) (durationSec * 1000 * 0.25));
            timestamps.add((long) (durationSec * 1000 * 0.5));
            timestamps.add((long) (durationSec * 1000 * 0.75));
            timestamps.add(Math.max(0, (durationSec - 1) * 1000L));
        } else {
            timestamps.add(1000L);
        }

        // Limit to maxFrames
        List<Long> sampled = timestamps.stream().distinct().limit(maxFrames).toList();

        List<VideoModerationFrame> frames = new ArrayList<>();
        for (int i = 0; i < sampled.size(); i++) {
            frames.add(VideoModerationFrame.builder()
                    .video(video)
                    .frameIndex(i)
                    .timestampMs(sampled.get(i))
                    .riskScore(0.0) // LOCAL_RULES cannot assess images
                    .categoriesJson("[]")
                    .build());
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
                .frames(frames.stream().map(f -> VideoModerationFrameResponseDTO.builder()
                        .frameIndex(f.getFrameIndex())
                        .timestampMs(f.getTimestampMs())
                        .riskScore(f.getRiskScore())
                        .categoriesJson(f.getCategoriesJson())
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
}
