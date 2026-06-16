package com.back.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class OAuth2RedirectBaseCookieService {

    private static final String COOKIE_NAME = "oauth2_redirect_base";
    private static final int MAX_AGE_SECONDS = 300;

    private final FrontendProperties frontendProperties;

    public OAuth2RedirectBaseCookieService(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }

    public void save(HttpServletResponse response, String redirectBase) {
        if (!frontendProperties.isAllowedUrl(redirectBase)) {
            return;
        }

        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(redirectBase.getBytes(StandardCharsets.UTF_8));
        response.addHeader("Set-Cookie", "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax"
                .formatted(COOKIE_NAME, value, MAX_AGE_SECONDS));
    }

    public String resolveOrDefault(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return frontendProperties.getPrimaryUrl();
        }

        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String redirectBase = new String(
                        Base64.getUrlDecoder().decode(cookie.getValue()),
                        StandardCharsets.UTF_8
                );
                if (frontendProperties.isAllowedUrl(redirectBase)) {
                    return redirectBase;
                }
            }
        }

        return frontendProperties.getPrimaryUrl();
    }

    public void clear(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax".formatted(COOKIE_NAME));
    }
}
