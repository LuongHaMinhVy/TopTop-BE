package com.back.config;

import com.back.auth.mapper.AuthResponseMapper;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.security.jwt.JwtService;
import com.back.common.service.cookieservice.ICookieService;
import com.back.common.utils.Translator;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.model.enums.UserStatus;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final IUserRepo userRepo;
    private final ICookieService ICookieService;
    private final OAuth2StateCache oAuth2StateCache;
    private final AuthResponseMapper authResponseMapper;
    private final FrontendProperties frontendProperties;
    private final OAuth2RedirectBaseCookieService redirectBaseCookieService;

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

        boolean reactivationRequired = false;
        if (user.getDeletedAt() != null) {
            if (user.getDeletionScheduledAt() != null
                    && !user.getDeletionScheduledAt().isAfter(LocalDateTime.now())) {
                redirectWithError(request, response, ErrorCode.ACCOUNT_BANNED.getMessage());
                return;
            }
            reactivationRequired = true;
        }

        if (!reactivationRequired && user.getStatus() != UserStatus.ACTIVE) {
            ErrorCode errorCode = user.getStatus() == UserStatus.BANNED
                    ? ErrorCode.ACCOUNT_BANNED
                    : ErrorCode.ACCOUNT_SUSPENDED;
            redirectWithError(request, response, accountStatusMessage(errorCode, user.getStatusReason()));
            return;
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = authResponseMapper.toAuthResponse(user, accessToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setReactivationRequired(reactivationRequired);
        authResponse.setReactivationReason(reactivationRequired
                ? (user.getDeletionScheduledAt() == null ? "DEACTIVATED" : "PENDING_DELETION")
                : null);
        authResponse.setDeletionScheduledAt(reactivationRequired ? user.getDeletionScheduledAt() : null);

        String stateKey = oAuth2StateCache.store(authResponse);

        ICookieService.add(response, "refreshToken", refreshToken,
                (int)(refreshTokenExpiration / 1000), request);

        String redirectUrl = redirectBaseCookieService.resolveOrDefault(request) + "/oauth2/callback?state=" + stateKey;
        redirectBaseCookieService.clear(response);
        log.info("OAuth2 login success for: {}", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(redirectBaseCookieService.resolveOrDefault(request) + "/oauth2/callback")
                .queryParam("error", "oauth2_failed")
                .queryParam("message", message)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        redirectBaseCookieService.clear(response);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String accountStatusMessage(ErrorCode errorCode, String reason) {
        if (reason == null || reason.isBlank()) {
            return errorCode.getMessage();
        }
        return errorCode.getMessage() + ". "
                + Translator.toLocale("error.account_status_reason_prefix", "Reason")
                + ": " + reason.trim();
    }
}
