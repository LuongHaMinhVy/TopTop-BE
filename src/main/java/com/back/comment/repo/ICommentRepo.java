package com.back.comment.repo;

import com.back.comment.model.entity.Comment;
import com.back.video.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ICommentRepo extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "WHERE c.video = :video " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findByVideoWithUser(@Param("video") Video video);

}
