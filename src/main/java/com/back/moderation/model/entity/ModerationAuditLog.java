package com.back.moderation.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.moderation.model.enums.ModerationActorType;
import com.back.moderation.model.enums.ModerationAuditAction;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "moderation_audit_logs")
public class ModerationAuditLog extends BaseEntity {

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ModerationActorType actorType;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ModerationAuditAction action;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_message", columnDefinition = "TEXT")
    private String reasonMessage;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
