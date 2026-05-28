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

    @Modifying
    void deleteByVideoId(Long videoId);

    @Modifying
    void deleteByUserId(Long userId);
}
