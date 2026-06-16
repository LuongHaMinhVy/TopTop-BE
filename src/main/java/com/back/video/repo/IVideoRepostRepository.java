package com.back.video.repo;

import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoRepost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVideoRepostRepository extends JpaRepository<VideoRepost, Long> {
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    Optional<VideoRepost> findByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    void deleteByVideoId(Long videoId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT vr FROM VideoRepost vr WHERE vr.video.id = :videoId ORDER BY vr.createdAt DESC")
    List<VideoRepost> findRecentByVideoId(@Param("videoId") Long videoId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT vr FROM VideoRepost vr WHERE vr.video.id = :videoId ORDER BY vr.createdAt DESC")
    List<VideoRepost> findByVideoIdWithUser(@Param("videoId") Long videoId);

    @Query("""
            SELECT vr.video FROM VideoRepost vr
            WHERE LOWER(vr.user.username) = LOWER(:username)
              AND vr.video.deletedAt IS NULL
              AND (
                    :viewerId IS NOT NULL
                    AND vr.video.user.id = :viewerId
                    OR (
                        vr.video.moderationStatus = com.back.moderation.model.enums.VideoModerationStatus.APPROVED
                    )
              )
              AND (
                (
                    vr.video.visibility = com.back.video.model.enums.VideoVisibility.PUBLIC
                    AND (
                        :viewerId IS NULL
                        OR vr.video.user.id = :viewerId
                        OR NOT EXISTS (
                            SELECT b.id FROM UserBlock b
                            WHERE (b.blocker.id = :viewerId AND b.blocked = vr.video.user)
                               OR (b.blocked.id = :viewerId AND b.blocker = vr.video.user)
                        )
                    )
                )
                OR (
                    :viewerId IS NOT NULL
                    AND vr.video.user.id = :viewerId
                )
                OR (
                    :viewerId IS NOT NULL
                    AND vr.video.visibility = com.back.video.model.enums.VideoVisibility.FRIENDS
                    AND NOT EXISTS (
                        SELECT b.id FROM UserBlock b
                        WHERE (b.blocker.id = :viewerId AND b.blocked = vr.video.user)
                           OR (b.blocked.id = :viewerId AND b.blocker = vr.video.user)
                    )
                    AND EXISTS (
                        SELECT f1.id FROM Follow f1
                        WHERE f1.follower.id = :viewerId AND f1.following.id = vr.video.user.id
                    )
                    AND EXISTS (
                        SELECT f2.id FROM Follow f2
                        WHERE f2.follower.id = vr.video.user.id AND f2.following.id = :viewerId
                    )
                )
              )
            ORDER BY vr.createdAt DESC
            """)
    Page<Video> findRepostedVideosByUsernameVisibleForViewer(
            @Param("username") String username,
            @Param("viewerId") Long viewerId,
            Pageable pageable);
}
