package com.back.video.repo;

import com.back.video.model.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IVideoLikeRepository extends JpaRepository<VideoLike, Long> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    Optional<VideoLike> findByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    void deleteByVideoId(Long videoId);
}
