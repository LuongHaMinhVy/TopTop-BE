package com.back.comment.repo;

import com.back.comment.model.entity.Comment;
import com.back.comment.model.enums.CommentStatus;
import com.back.video.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ICommentRepo extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "WHERE c.video = :video " +
            "AND c.parent IS NULL " +
            "AND (c.status IS NULL OR c.status <> com.back.comment.model.enums.CommentStatus.DELETED) " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findByVideoWithUser(@Param("video") Video video);

    @Query(value = "SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "WHERE c.video = :video " +
            "AND c.parent IS NULL " +
            "AND (c.status IS NULL OR c.status <> com.back.comment.model.enums.CommentStatus.DELETED) " +
            "ORDER BY c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.video = :video AND c.parent IS NULL AND (c.status IS NULL OR c.status <> com.back.comment.model.enums.CommentStatus.DELETED)")
    Page<Comment> findByVideoWithUser(@Param("video") Video video, Pageable pageable);

    @Query(value = "SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "WHERE c.parent = :parent " +
            "AND (c.status IS NULL OR c.status <> com.back.comment.model.enums.CommentStatus.DELETED) " +
            "ORDER BY c.createdAt ASC",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.parent = :parent AND (c.status IS NULL OR c.status <> com.back.comment.model.enums.CommentStatus.DELETED)")
    Page<Comment> findRepliesWithUser(@Param("parent") Comment parent, Pageable pageable);

    long countByParentAndStatusNot(Comment parent, CommentStatus status);
}
