package com.back.config;

import com.back.auth.model.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2StateCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "oauth2_state:";

    public String store(AuthResponse authResponse) {
        String key = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + key, authResponse, 5, TimeUnit.MINUTES);
        return key;
    }
    
    public AuthResponse consumeAndRemove(String key) {
        String redisKey = KEY_PREFIX + key;
        AuthResponse response = (AuthResponse) redisTemplate.opsForValue().get(redisKey);
        if (response != null) {
            redisTemplate.delete(redisKey);
        }
        return response;
    }
}