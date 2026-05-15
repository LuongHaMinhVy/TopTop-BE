package com.back.block.repo;

import com.back.block.model.entity.UserBlock;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserBlockRepo extends JpaRepository<UserBlock, Long> {
    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Page<UserBlock> findByBlocker(User blocker, Pageable pageable);
}
