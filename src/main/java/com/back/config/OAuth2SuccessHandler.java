package com.back.config;

import com.back.auth.mapper.AuthResponseMapper;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.security.jwt.JwtService;
import com.back.common.service.cookieservice.ICookieService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final IUserRepo userRepo;
    private final ICookieService ICookieService;
    private final OAuth2StateCache oAuth2StateCache;
    private final AuthResponseMapper authResponseMapper;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        if (oAuth2User == null) throw new AppException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);

        String email = oAuth2User.getAttribute("email");
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = authResponseMapper.toAuthResponse(user, accessToken);
        authResponse.setRefreshToken(refreshToken);

        String stateKey = oAuth2StateCache.store(authResponse);

        ICookieService.add(response, "refreshToken", refreshToken,
                (int)(refreshTokenExpiration / 1000), request);

        String redirectUrl = frontendUrl + "/oauth2/callback?state=" + stateKey;
        log.info("OAuth2 login success for: {}", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}