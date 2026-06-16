package com.back.user.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user_content_filter_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_content_filter_tag", columnNames = {"user_id", "tag"}),
        indexes = @Index(name = "idx_user_content_filter_user", columnList = "user_id")
)
public class UserContentFilterTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String tag;

    @Column(name = "sample_thumbnail_url")
    private String sampleThumbnailUrl;
}
