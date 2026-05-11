package com.back.auth.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResult{
    private AuthResponse authResponse;
    private String refreshToken;
    private Long refreshTokenExpiresIn;
    
    private String sessionId;
    private String deviceId;
}