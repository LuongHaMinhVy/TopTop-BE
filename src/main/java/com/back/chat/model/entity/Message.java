package com.back.chat.model.entity;

import com.back.chat.model.enums.MessageStatus;
import com.back.chat.model.enums.MessageType;
import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
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
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at")
})
@SQLRestriction("deleted_at IS NULL")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "client_message_id")
    private String clientMessageId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "hidden_for_user_ids", columnDefinition = "TEXT")
    private String hiddenForUserIds;
}
