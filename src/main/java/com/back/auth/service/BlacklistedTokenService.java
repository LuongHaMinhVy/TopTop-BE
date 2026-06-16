package com.back.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BlacklistedTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "blacklist:";

    public void add(String token, Instant expiredAt) {
        long duration = Duration.between(Instant.now(), expiredAt).toMillis();
        if (duration > 0) {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, "true", duration, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }

    public void deleteExpiredTokens() {
        // No-op: Redis handles TTL automatically
    }
}