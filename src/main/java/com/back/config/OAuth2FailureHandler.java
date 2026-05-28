package com.back.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final FrontendProperties frontendProperties;
    private final OAuth2RedirectBaseCookieService redirectBaseCookieService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 login failed: {}", exception.getMessage());
        String redirectUrl = UriComponentsBuilder
                .fromUriString(redirectBaseCookieService.resolveOrDefault(request) + "/oauth2/callback")
                .queryParam("error", "oauth2_failed")
                .queryParam("message", exception.getMessage())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        redirectBaseCookieService.clear(response);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
