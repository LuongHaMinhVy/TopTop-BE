package com.back.auth.repo;

import com.back.auth.model.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;

public interface IBlacklistedTokenRepo extends JpaRepository<BlacklistedToken, Long>{
    boolean existsByTokenHash(String token);

    void deleteByExpiredAtBefore(Instant time);
}