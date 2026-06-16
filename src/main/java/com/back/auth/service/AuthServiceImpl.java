package com.back.auth.service;

import com.back.auth.model.dto.request.LoginRequest;
import com.back.auth.model.dto.request.RegisterRequest;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.model.dto.response.AuthResult;
import com.back.auth.security.jwt.JwtService;
import com.back.common.service.cookieservice.ICookieService;
import com.back.common.service.emailservice.EmailService;
import com.back.common.utils.Translator;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.config.FrontendProperties;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.*;
import com.back.user.model.enums.AccountType;
import com.back.user.model.enums.RoleName;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IRoleRepo;
import com.back.user.repo.IUserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserRepo userRepo;
    private final IRoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final ICookieService ICookieService;
    private final BlacklistedTokenService blacklistedTokenService;
    private final VerificationTokenService verificationTokenService;
    private final UserInfoMapper userInfoMapper;
    private final FrontendProperties frontendProperties;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Transactional
    @Override
    public AuthResult login(LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        User user = userRepo.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.WRONG_EMAIL_OR_PASSWORD));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_EMAIL_OR_PASSWORD);
        }

        boolean reactivationRequired = false;
        if (user.getDeletedAt() != null) {
            if (user.getDeletionScheduledAt() != null && !user.getDeletionScheduledAt().isAfter(LocalDateTime.now())) {
                throw new AppException(ErrorCode.ACCOUNT_BANNED);
            }
            reactivationRequired = true;
        }

        if (!reactivationRequired && user.getStatus().equals(UserStatus.BANNED)) {
            throw accountStatusException(user);
        }

        if (!reactivationRequired && user.getStatus().equals(UserStatus.SUSPENDED)) {
            throw accountStatusException(user);
        }
        if (!user.getVerified() && !user.getEmail().endsWith("@example.com")) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // Check app type and roles
        String appId = request.getHeader("X-App-Id");
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleName.ROLE_ADMIN));

        if ("toptopuser".equals(appId) && isAdmin) {
            throw new AppException(ErrorCode.ADMIN_LOGIN_NOT_ALLOWED);
        }

        if ("toptopadmin".equals(appId) && !isAdmin) {
            throw new AppException(ErrorCode.USER_LOGIN_NOT_ALLOWED);
        }

        userRepo.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        ICookieService.add(response, "accessToken", accessToken,
                (int)(accessTokenExpiration / 1000), request);
        ICookieService.add(response, "refreshToken", refreshToken,
                (int)(refreshTokenExpiration / 1000), request);

        UserInfo userInfo = userInfoMapper.buildUserInfo(user);

        AuthResponse authResponse = AuthResponse.builder()
                .user(userInfo)
                .reactivationRequired(reactivationRequired)
                .reactivationReason(reactivationRequired ? reactivationReason(user) : null)
                .deletionScheduledAt(reactivationRequired ? user.getDeletionScheduledAt() : null)
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .refreshTokenExpiresIn(refreshTokenExpiration / 1000)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse reactivateAccount(HttpServletRequest request, HttpServletResponse response) {
        String token = ICookieService.get(request, "accessToken");
        if (token == null) {
            token = jwtService.extractFromHeader(request);
        }
        if (token == null || !jwtService.isAccessTokenValid(token)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = jwtService.extractUsername(token);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null
                && user.getDeletionScheduledAt() != null
                && !user.getDeletionScheduledAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        user.setDeletedAt(null);
        user.setDeletionScheduledAt(null);
        user.setStatus(UserStatus.ACTIVE);
        user.setStatusReason(null);
        user = userRepo.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        ICookieService.add(response, "accessToken", accessToken,
                (int)(accessTokenExpiration / 1000), request);
        ICookieService.add(response, "refreshToken", refreshToken,
                (int)(refreshTokenExpiration / 1000), request);

        return AuthResponse.builder()
                .user(userInfoMapper.buildUserInfo(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .reactivationRequired(false)
                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = ICookieService.get(request, "accessToken");
        if (accessToken == null) {
            accessToken = jwtService.extractFromHeader(request);
        }
        if (accessToken != null) {
            Instant expiryTime = jwtService.getExpirationTime(accessToken);
            blacklistedTokenService.add(accessToken, expiryTime);
            log.info("Access token blacklisted successfully");
        }
        ICookieService.clear(response, "accessToken", request);
        ICookieService.clear(response, "refreshToken", request);
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ICookieService.clear(response, "JSESSIONID", request);

        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (isReservedUserUsername(registerRequest.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (userRepo.existsByEmail(registerRequest.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepo.existsByUsername(registerRequest.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role userRole = roleRepo.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        boolean isTestEmail = registerRequest.getEmail().endsWith("@example.com");
        User newUser = User.builder()
                .username(registerRequest.getUsername())
                .nickname(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .verified(isTestEmail)
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .followersCount(0L)
                .followingCount(0L)
                .totalLikes(0L)
                .isPrivate(false)
                .accountType(AccountType.PERSONAL)
                .allowComments(true)
                .allowDuet(true)
                .allowStitch(true)
                .allowDownload(true)
                .allowMessageFromEveryone(false)
                .dateOfBirth(LocalDate.parse(registerRequest.getDateOfBirth()))
                .build();

        User savedUser = userRepo.save(newUser);
        log.info("User registered successfully: {}", savedUser.getEmail());

        if (!isTestEmail) {
            sendVerificationEmail(savedUser);
        }

    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String email = verificationTokenService.validateToken(token);
        if (email == null) {
            throw new AppException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        if (user.getVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        user.setVerified(true);
        userRepo.save(user);

        verificationTokenService.deleteToken(token);

        log.info("Email verified successfully for user: {}", email);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        verificationTokenService.deleteByEmail(email);

        sendVerificationEmail(user);
        log.info("Verification email resent to: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String resetToken = UUID.randomUUID().toString();
        verificationTokenService.savePasswordResetToken(email, resetToken);

        String resetLink = frontendProperties.getPrimaryUrl() + "/reset-password?token=" + resetToken;
        String subject = Translator.toLocale("email.forgot_password.subject");
        String htmlContent = buildPasswordResetEmail(user.getNickname(), resetLink);

        emailService.sendHtmlEmail(email, subject, htmlContent);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = verificationTokenService.validatePasswordResetToken(token);
        if (email == null) {
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        verificationTokenService.deleteToken(token);

        log.info("Password reset successfully for user: {}", email);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = ICookieService.get(request, "refreshToken");
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        ICookieService.add(response, "accessToken", newAccessToken, (int)(accessTokenExpiration / 1000), request);
        
        UserInfo userInfo = userInfoMapper.buildUserInfo(user);

        return AuthResponse.builder()
                .user(userInfo)
                .build();
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        verificationTokenService.saveVerificationToken(user.getEmail(), token);

        String verificationLink = frontendProperties.getPrimaryUrl() + "/verify-email?token=" + token;
        String subject = Translator.toLocale("email.verify.subject");
        String htmlContent = buildVerificationEmail(user.getNickname(), verificationLink);

        emailService.sendHtmlEmail(user.getEmail(), subject, htmlContent);
        log.info("Verification email sent to: {}", user.getEmail());
    }

    private String buildVerificationEmail(String nickname, String verificationLink) {
        String greeting = Translator.toLocale("email.verify.greeting", new Object[]{nickname});
        String body = Translator.toLocale("email.verify.body");
        String button = Translator.toLocale("email.verify.button");
        String copyLink = Translator.toLocale("email.verify.copy_link");
        String expiry = Translator.toLocale("email.verify.expiry");
        String footerIgnore = Translator.toLocale("email.verify.footer_ignore");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #FE2C55, #000); color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9f9f9; }
                        .button { display: inline-block; padding: 12px 30px; background: #FE2C55; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎵 TopTop</h1>
                        </div>
                        <div class="content">
                            <h2>%s</h2>
                            <p>%s</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">%s</a>
                            </p>
                            <p>%s</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 TikTok. All rights reserved.</p>
                            <p>%s</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(greeting, body, verificationLink, button, copyLink, verificationLink, expiry, footerIgnore);
    }

    private String buildPasswordResetEmail(String nickname, String resetLink) {
        String greeting = Translator.toLocale("email.reset_password.greeting", new Object[]{nickname});
        String body = Translator.toLocale("email.reset_password.body");
        String button = Translator.toLocale("email.reset_password.button");
        String copyLink = Translator.toLocale("email.reset_password.copy_link");
        String warningTitle = Translator.toLocale("email.reset_password.warning_title");
        String warningExpiry = Translator.toLocale("email.reset_password.warning_expiry");
        String warningOnce = Translator.toLocale("email.reset_password.warning_once");
        String warningNoShare = Translator.toLocale("email.reset_password.warning_no_share");
        String footerIgnore = Translator.toLocale("email.reset_password.footer_ignore");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #FE2C55, #000); color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9f9f9; }
                        .button { display: inline-block; padding: 12px 30px; background: #FE2C55; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔒 %s</h1>
                        </div>
                        <div class="content">
                            <h2>%s</h2>
                            <p>%s</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">%s</a>
                            </p>
                            <p>%s</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <div class="warning">
                                <strong>⚠️ %s</strong>
                                <ul>
                                    <li>%s</li>
                                    <li>%s</li>
                                    <li>%s</li>
                                </ul>
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2024 TikTok. All rights reserved.</p>
                            <p><strong>%s</strong></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(button, greeting, body, resetLink, button, copyLink, resetLink,
                        warningTitle, warningExpiry, warningOnce, warningNoShare, footerIgnore);
    }

    @Transactional
    @Override
    public AuthResponse onboardOAuth2(com.back.auth.model.dto.request.OAuth2OnboardRequest onboardRequest, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email;
        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getOnboarded()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "User is already onboarded");
        }

        if (isReservedUserUsername(onboardRequest.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (!user.getUsername().equals(onboardRequest.getUsername()) && userRepo.existsByUsername(onboardRequest.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        user.setUsername(onboardRequest.getUsername());
        user.setDateOfBirth(LocalDate.parse(onboardRequest.getDateOfBirth()));
        user.setPassword(passwordEncoder.encode(onboardRequest.getPassword()));
        user.setOnboarded(true);

        user = userRepo.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        ICookieService.add(servletResponse, "accessToken", accessToken,
                (int)(accessTokenExpiration / 1000), servletRequest);
        ICookieService.add(servletResponse, "refreshToken", refreshToken,
                (int)(refreshTokenExpiration / 1000), servletRequest);

        UserInfo userInfo = userInfoMapper.buildUserInfo(user);

        return AuthResponse.builder()
                .user(userInfo)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .build();
    }

    private boolean isReservedUserUsername(String username) {
        return username != null && "admin".equalsIgnoreCase(username.trim());
    }

    private AppException accountStatusException(User user) {
        ErrorCode errorCode = user.getStatus() == UserStatus.BANNED
                ? ErrorCode.ACCOUNT_BANNED
                : ErrorCode.ACCOUNT_SUSPENDED;
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

    private String reactivationReason(User user) {
        return user.getDeletionScheduledAt() == null ? "DEACTIVATED" : "PENDING_DELETION";
    }
}
