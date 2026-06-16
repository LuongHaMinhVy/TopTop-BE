package com.back.livestream.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.livestream.model.enums.LivestreamStatus;
import com.back.livestream.model.enums.LivestreamVisibility;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "livestreams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Livestream extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private LiveCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LivestreamStatus status = LivestreamStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LivestreamVisibility visibility = LivestreamVisibility.PUBLIC;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowChat = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowGifts = true;

    @Column(length = 200)
    private String roomName;

    @Column(nullable = false)
    @Builder.Default
    private int viewerCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int peakViewerCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long giftCount = 0;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
