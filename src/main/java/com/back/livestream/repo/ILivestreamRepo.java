package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.enums.LivestreamStatus;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ILivestreamRepo extends JpaRepository<Livestream, Long> {

    List<Livestream> findAllByHostOrderByCreatedAtDesc(User host);

    @Query("""
            SELECT l FROM Livestream l
            JOIN FETCH l.host h
            WHERE l.status = :status
              AND l.visibility = 'PUBLIC'
            ORDER BY l.viewerCount DESC, l.startedAt DESC
            """)
    Page<Livestream> findPublicLiveFeed(@Param("status") LivestreamStatus status, Pageable pageable);

    @Query("""
            SELECT l FROM Livestream l
            JOIN FETCH l.host h
            WHERE l.status = :status
              AND (l.visibility = 'PUBLIC'
                OR (l.visibility = 'FOLLOWERS_ONLY' AND EXISTS (
                    SELECT f FROM Follow f WHERE f.follower = :viewer AND f.following = h
                )))
            ORDER BY l.viewerCount DESC, l.startedAt DESC
            """)
    Page<Livestream> findVisibleFeedForUser(@Param("status") LivestreamStatus status,
                                             @Param("viewer") User viewer,
                                             Pageable pageable);

    boolean existsByHostAndStatus(User host, LivestreamStatus status);

    List<Livestream> findByStatus(LivestreamStatus status);
}
