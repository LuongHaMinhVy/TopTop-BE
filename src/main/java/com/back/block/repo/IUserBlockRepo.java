package com.back.block.repo;

import com.back.block.model.entity.UserBlock;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserBlockRepo extends JpaRepository<UserBlock, Long> {
    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Page<UserBlock> findByBlocker(User blocker, Pageable pageable);

    @Query("""
            SELECT b FROM UserBlock b
            WHERE b.blocker = :blocker
              AND LOWER(b.blocked.username) <> 'admin'
              AND NOT EXISTS (
                  SELECT r.id FROM b.blocked.roles r
                  WHERE r.name = com.back.user.model.enums.RoleName.ROLE_ADMIN
              )
            """)
    Page<UserBlock> findPublicBlockedByBlocker(@Param("blocker") User blocker, Pageable pageable);
}
