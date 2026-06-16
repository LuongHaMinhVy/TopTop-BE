package com.back.video.repo;

import com.back.video.model.entity.VideoNotInterested;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVideoNotInterestedRepository extends JpaRepository<VideoNotInterested, Long> {
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    @Query("SELECT vni.video.id FROM VideoNotInterested vni WHERE vni.user.id = :userId")
    List<Long> findVideoIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT COALESCE(v.aiCategory, v.category) FROM VideoNotInterested vni JOIN vni.video v WHERE vni.user.id = :userId AND (v.aiCategory IS NOT NULL OR v.category IS NOT NULL)")
    List<String> findAvoidCategoriesByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT v.videoCategory.id FROM VideoNotInterested vni JOIN vni.video v WHERE vni.user.id = :userId AND v.videoCategory.id IS NOT NULL")
    List<Long> findAvoidCategoryIdsByUserId(@Param("userId") Long userId);

    @Modifying
    void deleteByVideoId(Long videoId);

    @Modifying
    void deleteByUserId(Long userId);
}
