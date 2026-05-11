package com.back.config;

import com.back.auth.service.BlacklistedTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlackListSchedule {

    private final BlacklistedTokenService service;

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteBlacklistedTokens() {
        service.deleteExpiredTokens();
    }
}