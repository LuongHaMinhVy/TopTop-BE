package com.back.config;

import com.back.auth.service.CustomOAuth2UserService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.auth.security.jwt.JwtAuthFilter;
import com.back.user.model.enums.RoleName;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FrontendProperties.class)
public class SecurityConfig {
    
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler   oauth2SuccessHandler;
    private final OAuth2FailureHandler   oauth2FailureHandler;
    private final FrontendProperties frontendProperties;
    private final OAuth2RedirectBaseFilter oauth2RedirectBaseFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/login/oauth2/**",
                                "/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/collections").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").permitAll()
                        .requestMatchers("/api/v1/blocks/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Video endpoints ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/{id}/description-translation").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/user/{userId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/@{username}/{videoId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/{videoId}/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/videos/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/videos/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Comment endpoints ────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/video/{videoId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/{id}/replies").permitAll()
                        .requestMatchers("/api/v1/comments/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name(), RoleName.ROLE_MODERATOR.name())

                        // ── Collection endpoints ─────────────────────────────────
                        .requestMatchers("/api/v1/collections/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Following endpoints ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/following/suggestions").permitAll()
                        .requestMatchers("/api/v1/following/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Friends endpoints ────────────────────────────────────
                        .requestMatchers("/api/v1/friends/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Search endpoints ─────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/top").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/videos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/live").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/suggestions").permitAll()
                        .requestMatchers("/api/v1/search/history", "/api/v1/search/history/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Sound endpoints ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/sounds/favorites")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/sounds/*/save")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sounds/*/save")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/sounds/**").permitAll()

                        // ── Track endpoints ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/tracks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tracks/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tracks/**")
                        .hasAuthority(RoleName.ROLE_ADMIN.name())

                        // ── Report endpoints ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/reasons/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        .requestMatchers("/api/v1/admin/**")
                        .hasAuthority(RoleName.ROLE_ADMIN.name())

                        // ── Livestream endpoints ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/lives/feed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lives/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lives/*/chat/messages").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lives/gifts/catalog").permitAll()
                        // LiveKit webhook: verified by JWT signature inside the controller
                        .requestMatchers(HttpMethod.POST, "/api/v1/lives/livekit/webhook").permitAll()
                        .requestMatchers("/api/v1/lives/**")
                        .hasAnyAuthority(RoleName.ROLE_USER.name(), RoleName.ROLE_ADMIN.name())

                        // ── Shop endpoints ────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/slug/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shop-links/video/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shop-links/livestream/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, ex1) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            ApiResponse<Object> body = ApiResponse.builder()
                                    .message(Translator.toLocale("error.unauthorized_login", "Unauthorized: Please login"))
                                    .status(401)
                                    .timestamp(LocalDateTime.now())
                                    .build();

                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                        .accessDeniedHandler((request, response, ex2) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");

                            ApiResponse<Object> body = ApiResponse.builder()
                                    .message(Translator.toLocale("error.forbidden_permission", "Forbidden"))
                                    .status(403)
                                    .timestamp(LocalDateTime.now())
                                    .build();

                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                )

                .formLogin(AbstractHttpConfigurer::disable)

                .addFilterBefore(oauth2RedirectBaseFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(frontendProperties.getUrls());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
