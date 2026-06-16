package com.back.auth.controller;

import com.back.auth.model.dto.response.AuthResponse;
import com.back.common.service.cookieservice.ICookieService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.config.OAuth2StateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final OAuth2StateCache oAuth2StateCache;
    private final ICookieService ICookieService;

    @org.springframework.beans.factory.annotation.Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @org.springframework.beans.factory.annotation.Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @GetMapping("/exchange")
    public ResponseEntity<AuthResponse> exchange(@RequestParam String state, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        AuthResponse authResponse = oAuth2StateCache.consumeAndRemove(state);
        if (authResponse == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // Set cookies with the correct X-App-Id suffix (if provided in header/param)
        if (authResponse.getAccessToken() != null) {
            ICookieService.add(response, "accessToken", authResponse.getAccessToken(),
                    (int)(accessTokenExpiration / 1000), request);
        }
        if (authResponse.getRefreshToken() != null) {
            ICookieService.add(response, "refreshToken", authResponse.getRefreshToken(),
                    (int)(refreshTokenExpiration / 1000), request);
        }

        return ResponseEntity.ok(authResponse);
    }
}