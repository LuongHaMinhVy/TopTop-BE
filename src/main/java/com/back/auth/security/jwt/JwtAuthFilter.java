package com.back.auth.security.jwt;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.service.cookieservice.ICookieService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.Translator;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IUserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final ICookieService ICookieService;
    private final ObjectMapper objectMapper;
    private final IUserRepo userRepo;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = ICookieService.get(request, "accessToken");

        if (token == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isAccessTokenValid(token)) {
                    User user = userRepo.findByEmail(email)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                    if (user.getStatus() != UserStatus.ACTIVE) {
                        ICookieService.clear(response, "accessToken", request);
                        ICookieService.clear(response, "refreshToken", request);
                        throw accountStatusException(user);
                    }

                    List<String> roles = jwtService.extractRolesFromToken(token);
                    
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    authorities
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (AppException e) {
            log.error("JWT Filter error [AppException]: {}", e.getMessage());
            SecurityContextHolder.clearContext();

            response.setStatus(e.getErrorCode().getStatus().value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.builder()
                    .message(e.getMessage())
                    .status(e.getErrorCode().getStatus().value())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), apiResponse);
            return;
        } catch (Exception e) {
            log.error("JWT Filter error [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.builder()
                    .message(Translator.toLocale("error.invalid_token", "Invalid token"))
                    .status(401)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), apiResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/v1/auth/");
    }

    private AppException accountStatusException(User user) {
        ErrorCode errorCode = user.getStatus() == UserStatus.BANNED
                ? ErrorCode.ACCOUNT_BANNED
                : user.getStatus() == UserStatus.SUSPENDED
                ? ErrorCode.ACCOUNT_SUSPENDED
                : ErrorCode.ACCOUNT_NOT_ACTIVE;
        return new AppException(errorCode, null, accountStatusMessage(errorCode, user.getStatusReason()));
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
