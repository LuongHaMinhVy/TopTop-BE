package com.back.video.repo;

import com.back.video.model.entity.VideoView;
import com.back.video.model.entity.Video;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IVideoViewRepository extends JpaRepository<VideoView, Long> {
    List<VideoView> findByOwnerIdAndCreatedAtBetween(Long ownerId, LocalDateTime start, LocalDateTime end);

    void deleteByVideoId(Long videoId);

    @Query("""
            SELECT v FROM VideoView vv
            JOIN vv.video v
            JOIN FETCH v.user
            WHERE vv.viewer.id = :viewerId
              AND v.deletedAt IS NULL
            ORDER BY vv.createdAt DESC
            """)
    List<Video> findRecentViewedVideosByViewerId(@Param("viewerId") Long viewerId, Pageable pageable);
}
