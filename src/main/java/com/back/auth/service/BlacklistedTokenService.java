package com.back.auth.service;

import com.back.auth.model.entity.BlacklistedToken;
import com.back.auth.repo.IBlacklistedTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BlacklistedTokenService {

    private final IBlacklistedTokenRepo repository;

    public void deleteExpiredTokens() {
        repository.deleteByExpiredAtBefore(Instant.now());
    }

    public void add(String token, Instant expiredAt) {
        BlacklistedToken entity = BlacklistedToken.builder()
                .tokenHash(token)
                .expiredAt(expiredAt)
                .build();

        repository.save(entity);
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByTokenHash(token);
    }


}