package com.back.comment.repo;

import com.back.comment.model.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ICommentLikeRepo extends JpaRepository<CommentLike, Long> {

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    @Query("""
            SELECT cl.comment.id
            FROM CommentLike cl
            WHERE cl.user.id = :userId
              AND cl.comment.id IN :commentIds
            """)
    Set<Long> findLikedCommentIdsByUser(
            @Param("userId") Long userId,
            @Param("commentIds") Collection<Long> commentIds);
}
