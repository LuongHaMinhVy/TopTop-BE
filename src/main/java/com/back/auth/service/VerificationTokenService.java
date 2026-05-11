package com.back.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationTokenService {

    private final Map<String, TokenData> verificationTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenData> passwordResetTokens = new ConcurrentHashMap<>();

    @Value("${spring.data.verification-expiration-hours}")
    private long verificationTokenExpirationHours;
    private static final long PASSWORD_RESET_EXPIRY_HOURS = 1;
    
    public void saveVerificationToken(String email, String token) {
        TokenData tokenData = new TokenData(email, Instant.now().plus(verificationTokenExpirationHours, ChronoUnit.HOURS));
        verificationTokens.put(token, tokenData);
    }
    
    public void savePasswordResetToken(String email, String token) {
        TokenData tokenData = new TokenData(email, Instant.now().plus(PASSWORD_RESET_EXPIRY_HOURS, ChronoUnit.HOURS));
        passwordResetTokens.put(token, tokenData);
    }
    
    public String validateToken(String token) {
        TokenData tokenData = verificationTokens.get(token);
        if (tokenData == null || tokenData.isExpired()) {
            return null;
        }
        return tokenData.email;
    }
    
    public String validatePasswordResetToken(String token) {
        TokenData tokenData = passwordResetTokens.get(token);
        if (tokenData == null || tokenData.isExpired()) {
            return null;
        }
        return tokenData.email;
    }
    
    public void deleteToken(String token) {
        verificationTokens.remove(token);
        passwordResetTokens.remove(token);
    }
    
    public void deleteByEmail(String email) {
        verificationTokens.entrySet().removeIf(entry -> entry.getValue().email.equals(email));
        passwordResetTokens.entrySet().removeIf(entry -> entry.getValue().email.equals(email));
    }
    
    private record TokenData(String email, Instant expiryTime) {
        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }
}