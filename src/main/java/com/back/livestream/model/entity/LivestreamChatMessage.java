package com.back.livestream.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.livestream.model.enums.ChatMessageType;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "livestream_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LivestreamChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livestream_id", nullable = false)
    private Livestream livestream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatMessageType messageType = ChatMessageType.CHAT;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPinned = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isHidden = false;
}
