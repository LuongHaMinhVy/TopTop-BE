package com.back.chat.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = "conversation_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
})
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String nickname;

    @Column
    @Builder.Default
    private String role = "MEMBER";

    @Column(name = "joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
