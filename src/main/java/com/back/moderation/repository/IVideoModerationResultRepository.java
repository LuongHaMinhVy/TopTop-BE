package com.back.moderation.repository;

import com.back.moderation.model.entity.VideoModerationResult;
import com.back.moderation.model.enums.VideoModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IVideoModerationResultRepository extends JpaRepository<VideoModerationResult, Long> {
    Optional<VideoModerationResult> findTopByVideoIdOrderByCreatedAtDesc(Long videoId);

    @Query("SELECT r FROM VideoModerationResult r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<VideoModerationResult> findByStatus(VideoModerationStatus status, Pageable pageable);

    @Query("SELECT r FROM VideoModerationResult r JOIN r.video v WHERE v.moderationStatus = :status ORDER BY r.createdAt DESC")
    Page<VideoModerationResult> findByVideoModerationStatus(VideoModerationStatus status, Pageable pageable);

    void deleteByVideoId(Long videoId);
}
