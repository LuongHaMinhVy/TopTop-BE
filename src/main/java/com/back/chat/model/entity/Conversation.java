package com.back.chat.model.entity;

import com.back.chat.model.enums.ConversationStatus;
import com.back.chat.model.enums.ConversationType;
import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = "conversations")
@SQLRestriction("deleted_at IS NULL")
public class Conversation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationType type = ConversationType.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_preview")
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "direct_key", unique = true)
    private String directKey;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
