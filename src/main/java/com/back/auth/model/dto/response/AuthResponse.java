package com.back.auth.model.dto.response;

import com.back.user.model.dto.response.UserInfo;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private UserInfo user;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}