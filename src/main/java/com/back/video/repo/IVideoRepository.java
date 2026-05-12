package com.back.video.repo;

import com.back.video.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IVideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserId(Long userId);

    @Query("SELECT v FROM Video v WHERE v.deletedAt < :cutoff")
    List<Video> findExpiredVideos(LocalDateTime cutoff);
}
