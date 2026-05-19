package com.back.follow.repo;

import com.back.follow.model.entity.Follow;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface IFollowRepo extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);
    boolean existsByFollowerAndFollowing(User follower, User following);
    java.util.List<Follow> findByFollower(User follower);
    Page<Follow> findByFollower(User follower, Pageable pageable);

    long countByFollower(User follower);
    long countByFollowing(User following);

    @Query("""
            SELECT COUNT(f.id) FROM Follow f
            WHERE f.follower.id = :userId
              AND EXISTS (
                  SELECT f2.id FROM Follow f2
                  WHERE f2.follower.id = f.following.id
                    AND f2.following.id = :userId
              )
            """)
    long countFriends(@Param("userId") Long userId);
}
