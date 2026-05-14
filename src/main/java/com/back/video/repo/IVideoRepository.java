package com.back.video.repo;

import com.back.video.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IVideoRepository extends JpaRepository<Video, Long> {
    
    @EntityGraph(attributePaths = {"user"})
    Page<Video> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Video> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.deletedAt < :cutoff")
    List<Video> findExpiredVideos(LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"user", "hashtags"})
    @Query("SELECT v FROM Video v WHERE LOWER(v.user.username) = LOWER(:username) AND v.id = :id")
    java.util.Optional<Video> findByUserUsernameAndId(String username, Long id);
}
