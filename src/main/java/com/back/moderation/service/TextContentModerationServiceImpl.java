package com.back.moderation.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.model.entity.ModerationAuditLog;
import com.back.moderation.model.enums.ModerationActorType;
import com.back.moderation.model.enums.ModerationAuditAction;
import com.back.moderation.repository.IModerationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TextContentModerationServiceImpl implements ITextContentModerationService {

    private final GeminiModerationProvider moderationProvider;
    private final IModerationAuditLogRepository auditLogRepository;

    @Value("${moderation.text.auto-reject-threshold:0.35}")
    private double autoRejectThreshold;

    @Override
    public void assertAllowed(String targetType, String text, Long actorUserId, String fieldName) {
        if ("MESSAGE".equals(targetType)) {
            return;
        }
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            return;
        }

        ModerationProviderResult result = moderationProvider.moderateText(
                new TextModerationInput(normalized, List.of())
        );

        if (result.riskScore() < autoRejectThreshold) {
            return;
        }

        auditLogRepository.save(ModerationAuditLog.builder()
                .targetType(targetType)
                .targetId(0L)
                .actorUserId(actorUserId)
                .actorType(ModerationActorType.SYSTEM)
                .previousStatus("SUBMITTED")
                .newStatus("REJECTED")
                .action(ModerationAuditAction.AUTO_REJECT)
                .reasonCode(result.reasonCode() != null ? result.reasonCode() : "TEXT_MODERATION_REJECTED")
                .reasonMessage(result.reasonMessage() != null
                        ? result.reasonMessage()
                        : "Nội dung bị từ chối bởi kiểm duyệt tự động.")
                .metadataJson("{\"riskScore\":" + result.riskScore() + "}")
                .build());

        ErrorCode errorCode = "MESSAGE".equals(targetType)
                ? ErrorCode.MESSAGE_REJECTED_BY_MODERATION
                : "COMMENT".equals(targetType)
                ? ErrorCode.COMMENT_REJECTED_BY_MODERATION
                : ErrorCode.VIDEO_TEXT_REJECTED_BY_MODERATION;
        throw new AppException(errorCode, fieldName);
    }
}
