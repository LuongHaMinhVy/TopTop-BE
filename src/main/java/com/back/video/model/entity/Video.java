package com.back.video.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.hashtag.model.entity.Hashtag;
import com.back.user.model.entity.User;
import com.back.video.model.enums.VideoVisibility;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "videos")
@SQLRestriction("deleted_at IS NULL")
public class Video extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String fileUrl;

    @Column
    private String thumbnailUrl;

    @Column
    private Integer duration;

    @Column
    private String category;

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long likeCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long commentCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long saveCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoVisibility visibility = VideoVisibility.PUBLIC;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowComments = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowEdit = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "video_hashtags",
        joinColumns = @JoinColumn(name = "video_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private java.util.Set<Hashtag> hashtags = new java.util.HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
