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

    @Query("""
            SELECT vv FROM VideoView vv
            JOIN FETCH vv.video v
            JOIN FETCH v.user
            WHERE vv.viewer.id = :viewerId
              AND v.deletedAt IS NULL
            ORDER BY vv.createdAt DESC
            """)
    List<VideoView> findRecentViewsWithRatesByViewerId(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
            SELECT vv.video.id FROM VideoView vv
            WHERE vv.viewer.id = :viewerId
            GROUP BY vv.video.id
            ORDER BY MAX(vv.createdAt) DESC
            """)
    List<Long> findRecentViewedVideoIdsByViewerId(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(vv.completionRate), 0.0) FROM VideoView vv WHERE vv.video.id = :videoId")
    Double getAverageCompletionRateByVideoId(@Param("videoId") Long videoId);

    @Query("SELECT COALESCE(AVG(vv.completionRate), 0.0) FROM VideoView vv WHERE vv.viewer.id = :viewerId")
    Double getAverageCompletionRateByViewerId(@Param("viewerId") Long viewerId);

    @Query("SELECT vv.video.id, COALESCE(AVG(vv.completionRate), 0.0) FROM VideoView vv WHERE vv.video.id IN :videoIds GROUP BY vv.video.id")
    List<Object[]> getAverageCompletionRates(@Param("videoIds") List<Long> videoIds);
}
