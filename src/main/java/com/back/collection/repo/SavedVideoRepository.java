package com.back.collection.repo;

import com.back.collection.model.entity.SavedVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedVideoRepository extends JpaRepository<SavedVideo, Long> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    Optional<SavedVideo> findByUserIdAndVideoId(Long userId, Long videoId);

    @EntityGraph(attributePaths = {"video", "video.user"})
    Page<SavedVideo> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"video", "video.user"})
    @Query("""
            SELECT sv FROM SavedVideo sv
            WHERE sv.user.id = :userId
              AND NOT EXISTS (
                    SELECT b.id FROM UserBlock b
                    WHERE (b.blocker.id = :userId AND b.blocked = sv.video.user)
                       OR (b.blocked.id = :userId AND b.blocker = sv.video.user)
              )
            ORDER BY sv.createdAt DESC
            """)
    Page<SavedVideo> findVisibleByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
