package com.back.collection.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(
        name = "video_collections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_video_collections_user_name", columnNames = {"user_id", "name"})
        },
        indexes = {
                @Index(name = "idx_video_collections_user", columnList = "user_id")
        }
)
public class VideoCollection extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 240)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPublic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
