package com.back.user.repo;

import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepo extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String usernameOrEmail);
    
    java.util.List<User> findTop10ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String username, String nickname);
    java.util.List<User> findTop10ByOrderByCreatedAtDesc();

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY
               CASE WHEN LOWER(u.username) = LOWER(:keyword) THEN 0 ELSE 1 END,
               CASE WHEN LOWER(u.username) LIKE LOWER(CONCAT(:keyword, '%')) THEN 0 ELSE 1 END,
               u.verified DESC,
               u.followersCount DESC,
               u.totalLikes DESC
            """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (:viewerId IS NULL OR u.id <> :viewerId)
              AND (:viewerId IS NULL OR NOT EXISTS (
                  SELECT f.id FROM Follow f
                  WHERE f.follower.id = :viewerId AND f.following.id = u.id
              ))
              AND (:viewerId IS NULL OR NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :viewerId AND b.blocked = u)
                     OR (b.blocked.id = :viewerId AND b.blocker = u)
              ))
            ORDER BY u.followersCount DESC, u.totalLikes DESC
            """)
    Page<User> findSuggestedUsersToFollow(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :viewerId
              AND NOT EXISTS (
                  SELECT f.id FROM Follow f
                  WHERE f.follower.id = :viewerId AND f.following.id = u.id
              )
              AND NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :viewerId AND b.blocked = u)
                     OR (b.blocked.id = :viewerId AND b.blocker = u)
              )
            ORDER BY 
              CASE WHEN EXISTS (
                  SELECT f2.id FROM Follow f2
                  WHERE f2.follower.id = u.id AND f2.following.id = :viewerId
              ) THEN 0 ELSE 1 END ASC,
              u.followersCount DESC, 
              u.totalLikes DESC
            """)
    Page<User> findSuggestedFriends(@Param("viewerId") Long viewerId, Pageable pageable);
}
