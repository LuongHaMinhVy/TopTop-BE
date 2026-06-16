package com.back.moderation.repository;

import com.back.moderation.model.entity.ModerationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, Long> {
    List<ModerationAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
