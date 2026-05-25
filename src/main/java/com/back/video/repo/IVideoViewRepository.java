package com.back.video.repo;

import com.back.video.model.entity.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IVideoViewRepository extends JpaRepository<VideoView, Long> {
    List<VideoView> findByOwnerIdAndCreatedAtBetween(Long ownerId, LocalDateTime start, LocalDateTime end);

    void deleteByVideoId(Long videoId);
}
