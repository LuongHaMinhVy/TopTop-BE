package com.back.video.repo;

import com.back.video.model.entity.Video;
import com.back.moderation.model.enums.VideoModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IVideoRepository extends JpaRepository<Video, Long> {
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT v FROM Video v WHERE v.deletedAt IS NULL AND v.user.deletedAt IS NULL")
    Page<Video> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT DISTINCT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND (
                (
                    v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
                    AND (
                        :viewerId IS NULL
                        OR v.user.id = :viewerId
                        OR NOT EXISTS (
                            SELECT b.id FROM UserBlock b
                            WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                               OR (b.blocked.id = :viewerId AND b.blocker = v.user)
                        )
                    )
                )
                OR (
                    :viewerId IS NOT NULL
                    AND v.user.id = :viewerId
                )
                OR (
                    :viewerId IS NOT NULL
                    AND v.visibility = com.back.video.model.enums.VideoVisibility.FRIENDS
                    AND NOT EXISTS (
                        SELECT b.id FROM UserBlock b
                        WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                           OR (b.blocked.id = :viewerId AND b.blocker = v.user)
                    )
                    AND EXISTS (
                        SELECT f1.id FROM Follow f1
                        WHERE f1.follower.id = :viewerId AND f1.following.id = v.user.id
                    )
                    AND EXISTS (
                        SELECT f2.id FROM Follow f2
                        WHERE f2.follower.id = v.user.id AND f2.following.id = :viewerId
                    )
                )
              )
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            """)
    Page<Video> findAllVisibleForViewer(@Param("viewerId") Long viewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT v FROM Video v WHERE v.user.id = :userId AND v.deletedAt IS NULL AND v.user.deletedAt IS NULL")
    Page<Video> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.user.id = :userId
              AND v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND (
                (
                    v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
                    AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
                    AND (
                        :viewerId IS NULL
                        OR v.user.id = :viewerId
                        OR NOT EXISTS (
                            SELECT b.id FROM UserBlock b
                            WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                               OR (b.blocked.id = :viewerId AND b.blocker = v.user)
                        )
                    )
                )
                OR (
                    :viewerId IS NOT NULL
                    AND v.user.id = :viewerId
                )
                OR (
                    :viewerId IS NOT NULL
                    AND v.visibility = com.back.video.model.enums.VideoVisibility.FRIENDS
                    AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
                    AND NOT EXISTS (
                        SELECT b.id FROM UserBlock b
                        WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                           OR (b.blocked.id = :viewerId AND b.blocker = v.user)
                    )
                    AND EXISTS (
                        SELECT f1.id FROM Follow f1
                        WHERE f1.follower.id = :viewerId AND f1.following.id = v.user.id
                    )
                    AND EXISTS (
                        SELECT f2.id FROM Follow f2
                        WHERE f2.follower.id = v.user.id AND f2.following.id = :viewerId
                    )
                )
              )
            """)
            Page<Video> findByUserIdVisibleForViewer(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.user.id IN (
                  SELECT f.following.id FROM Follow f
                  WHERE f.follower.id = :viewerId
              )
              AND (
                v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
                OR (
                    v.visibility = com.back.video.model.enums.VideoVisibility.FRIENDS
                    AND EXISTS (
                        SELECT f2.id FROM Follow f2
                        WHERE f2.follower.id = v.user.id AND f2.following.id = :viewerId
                    )
                )
              )
              AND NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                     OR (b.blocked.id = :viewerId AND b.blocker = v.user)
              )
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            ORDER BY v.createdAt DESC
            """)
    Page<Video> findFollowingFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.user.id IN (
                  SELECT f.following.id FROM Follow f
                  WHERE f.follower.id = :viewerId
                    AND EXISTS (
                        SELECT f2.id FROM Follow f2
                        WHERE f2.follower.id = f.following.id AND f2.following.id = :viewerId
                    )
              )
              AND (
                v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
                OR v.visibility = com.back.video.model.enums.VideoVisibility.FRIENDS
              )
              AND NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                     OR (b.blocked.id = :viewerId AND b.blocker = v.user)
              )
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            ORDER BY v.createdAt DESC
            """)
    Page<Video> findFriendsFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT v FROM Video v
            JOIN VideoLike vl ON vl.video = v
            WHERE vl.user.id = :userId
              AND v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND (
                    v.user.id = :userId
                    OR NOT EXISTS (
                        SELECT b.id FROM UserBlock b
                        WHERE (b.blocker.id = :userId AND b.blocked = v.user)
                           OR (b.blocked.id = :userId AND b.blocker = v.user)
                    )
              )
              AND (
                    v.user.id = :userId
                    OR (
                        v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
                    )
              )
            ORDER BY vl.id DESC
            """)
    Page<Video> findLikedVideosByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    long countByModerationStatus(VideoModerationStatus status);

    List<Video> findAllByUserId(Long userId);

    @Query("SELECT v FROM Video v WHERE v.deletedAt IS NOT NULL AND v.deletedAt < :cutoff")
    List<Video> findExpiredVideos(LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"user", "hashtags"})
    @Query("SELECT v FROM Video v WHERE LOWER(v.user.username) = LOWER(:username) AND v.id = :id AND v.deletedAt IS NULL AND v.user.deletedAt IS NULL")
    java.util.Optional<Video> findByUserUsernameAndId(String username, Long id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
              AND (
                    LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(COALESCE(v.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(v.user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(v.user.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
             ORDER BY v.likeCount DESC, v.viewCount DESC, v.createdAt DESC
            """)
    Page<Video> searchPublicVideos(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "sound"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.id IN :ids
              AND v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            """)
    List<Video> findPublicSearchVideosByIds(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"user", "sound"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            """)
    List<Video> findSearchIndexVideos();

    @EntityGraph(attributePaths = {"user", "sound"})
    @Query("""
            SELECT v FROM Video v
            WHERE v.deletedAt IS NULL
              AND v.user.deletedAt IS NULL
              AND v.sound.id = :soundId
              AND v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
              AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
            ORDER BY v.createdAt DESC
            """)
    Page<Video> findPublicVideosBySoundId(@Param("soundId") Long soundId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT v FROM Video v
        WHERE v.deletedAt IS NULL
          AND v.user.deletedAt IS NULL
          AND v.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
          AND v.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
          AND v.createdAt >= :cutoffTime
          AND (
              :viewerId IS NULL
              OR NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :viewerId AND b.blocked = v.user)
                     OR (b.blocked.id = :viewerId AND b.blocker = v.user)
              )
          )
        ORDER BY (v.likeCount * 2 + v.commentCount * 3 + v.saveCount * 4) DESC
    """)
    List<Video> findViralVideos(@Param("viewerId") Long viewerId, @Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);

    List<Video> findByVideoCategoryIsNull();
}
