package com.back.video.repo;

import com.back.video.model.entity.VideoLike;
import com.back.video.model.entity.Video;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVideoLikeRepository extends JpaRepository<VideoLike, Long> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    Optional<VideoLike> findByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    void deleteByVideoId(Long videoId);

    @Query("""
            SELECT v FROM VideoLike vl
            JOIN vl.video v
            JOIN FETCH v.user
            WHERE vl.user.id = :userId
              AND v.deletedAt IS NULL
            ORDER BY vl.createdAt DESC
            """)
    List<Video> findRecentLikedVideosByUserId(@Param("userId") Long userId, Pageable pageable);
}
