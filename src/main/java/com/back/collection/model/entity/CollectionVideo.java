package com.back.collection.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.video.model.entity.Video;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(
        name = "collection_videos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_collection_videos_collection_video", columnNames = {"collection_id", "video_id"})
        },
        indexes = {
                @Index(name = "idx_collection_videos_collection", columnList = "collection_id"),
                @Index(name = "idx_collection_videos_video", columnList = "video_id")
        }
)
public class CollectionVideo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private VideoCollection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();
}
