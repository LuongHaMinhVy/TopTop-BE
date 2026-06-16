package com.back.comment.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_like_user_comment", columnNames = {"user_id", "comment_id"}),
        indexes = {
                @Index(name = "idx_comment_like_comment_created", columnList = "comment_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommentLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
