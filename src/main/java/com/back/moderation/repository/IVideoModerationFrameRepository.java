package com.back.moderation.repository;

import com.back.moderation.model.entity.VideoModerationFrame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IVideoModerationFrameRepository extends JpaRepository<VideoModerationFrame, Long> {
    List<VideoModerationFrame> findByModerationResultId(Long moderationResultId);
    List<VideoModerationFrame> findByVideoIdOrderByFrameIndex(Long videoId);

    void deleteByVideoId(Long videoId);
}
