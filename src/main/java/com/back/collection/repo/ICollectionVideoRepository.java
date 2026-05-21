package com.back.collection.repo;

import com.back.collection.model.entity.CollectionVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Limit;

import java.util.Optional;

@Repository
public interface ICollectionVideoRepository extends JpaRepository<CollectionVideo, Long> {

    boolean existsByCollectionIdAndVideoId(Long collectionId, Long videoId);

    Optional<CollectionVideo> findByCollectionIdAndVideoId(Long collectionId, Long videoId);

    Long countByCollectionId(Long collectionId);

    @EntityGraph(attributePaths = {"video", "video.user"})
    Page<CollectionVideo> findByCollectionIdOrderByAddedAtDesc(Long collectionId, Pageable pageable);

    @EntityGraph(attributePaths = {"video", "video.user"})
    @Query("""
            SELECT cv FROM CollectionVideo cv
            WHERE cv.collection.id = :collectionId
              AND cv.collection.user.id = :userId
              AND NOT EXISTS (
                    SELECT b.id FROM UserBlock b
                    WHERE (b.blocker.id = :userId AND b.blocked = cv.video.user)
                       OR (b.blocked.id = :userId AND b.blocker = cv.video.user)
              )
            ORDER BY cv.addedAt DESC
            """)
    Page<CollectionVideo> findVisibleByCollectionIdOrderByAddedAtDesc(
            @Param("collectionId") Long collectionId,
            @Param("userId") Long userId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"video", "video.user"})
    @Query("""
            SELECT cv FROM CollectionVideo cv
            WHERE cv.collection.id = :collectionId
              AND cv.collection.user.id = :ownerId
              AND (
                    :viewerId IS NULL
                    OR cv.video.user.id = :viewerId
                    OR NOT EXISTS (
                        SELECT b.id FROM UserBlock b
                        WHERE (b.blocker.id = :viewerId AND b.blocked = cv.video.user)
                           OR (b.blocked.id = :viewerId AND b.blocker = cv.video.user)
                    )
              )
            ORDER BY cv.addedAt DESC
            """)
    Page<CollectionVideo> findVisibleByCollectionIdForViewerOrderByAddedAtDesc(
            @Param("collectionId") Long collectionId,
            @Param("ownerId") Long ownerId,
            @Param("viewerId") Long viewerId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"video"})
    Optional<CollectionVideo> findFirstByCollectionIdOrderByAddedAtDesc(Long collectionId);

    @EntityGraph(attributePaths = {"video"})
    @Query("""
            SELECT cv FROM CollectionVideo cv
            WHERE cv.collection.id = :collectionId
              AND cv.video.deletedAt IS NULL
            ORDER BY cv.addedAt DESC
            """)
    Optional<CollectionVideo> findFirstAvailableByCollectionIdOrderByAddedAtDesc(@Param("collectionId") Long collectionId, Limit limit);

    void deleteByCollectionId(Long collectionId);

    void deleteByVideoId(Long videoId);

    @Modifying
    @Query("DELETE FROM CollectionVideo cv WHERE cv.video.id = :videoId AND cv.collection.user.id = :userId")
    void deleteByVideoIdAndCollectionUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);
}
