package com.back.auth.service;

import com.back.auth.model.dto.request.LoginRequest;
import com.back.auth.model.dto.request.RegisterRequest;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.model.dto.response.AuthResult;
import com.back.auth.security.jwt.JwtService;
import com.back.common.service.cookieservice.CookieService;
import com.back.common.service.emailservice.EmailService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.*;
import com.back.user.repo.IRoleRepo;
import com.back.user.repo.IUserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserRepo userRepo;
    private final IRoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final BlacklistedTokenService blacklistedTokenService;
    private final VerificationTokenService verificationTokenService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public AuthResult login(LoginRequest loginRequest) {
        User user = userRepo.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.WRONG_EMAIL_OR_PASSWORD));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_EMAIL_OR_PASSWORD);
        }

        if (user.getStatus().equals(UserStatus.BANNED)) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        if (user.getStatus().equals(UserStatus.SUSPENDED)) {
            throw new AppException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (!user.getVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        userRepo.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserInfo userInfo = buildUserInfo(user);

        AuthResponse authResponse = AuthResponse.builder()
                .user(userInfo)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExpiration / 1000)
                .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtService.extractFromHeader(request);
        if (accessToken != null) {
            Instant expiryTime = jwtService.getExpirationTime(accessToken);
            blacklistedTokenService.add(accessToken, expiryTime);
            log.info("Access token blacklisted successfully");
        }
        cookieService.clear(response, "refreshToken");
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest registerRequest) {
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

        User newUser = User.builder()
                .username(registerRequest.getUsername())
                .nickname(registerRequest.getUsername()) // Mặc định nickname = username
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .verified(false)
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .followersCount(0L)
                .followingCount(0L)
                .totalLikes(0L)
                .videoCount(0L)
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

        sendVerificationEmail(savedUser);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        UserInfo userInfo = buildUserInfo(savedUser);

        AuthResponse authResponse = AuthResponse.builder()
                .user(userInfo)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExpiration / 1000)
                .build();
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

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Đặt lại mật khẩu TikTok";
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
    public AuthResponse refreshToken(String refreshToken) {
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
        UserInfo userInfo = buildUserInfo(user);

        return AuthResponse.builder()
                .user(userInfo)
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    private UserInfo buildUserInfo(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .totalLikes(user.getTotalLikes())
                .videoCount(user.getVideoCount())
                .verified(user.getVerified())
                .isPrivate(user.getIsPrivate())
                .status(user.getStatus().name())
                .accountType(user.getAccountType().name())
                .websiteUrl(user.getWebsiteUrl())
                .instagramHandle(user.getInstagramHandle())
                .youtubeHandle(user.getYoutubeHandle())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .region(user.getRegion())
                .dateOfBirth(user.getDateOfBirth())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        verificationTokenService.saveVerificationToken(user.getEmail(), token);

        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        String subject = "Xác thực email TikTok của bạn";
        String htmlContent = buildVerificationEmail(user.getNickname(), verificationLink);

        emailService.sendHtmlEmail(user.getEmail(), subject, htmlContent);
        log.info("Verification email sent to: {}", user.getEmail());
    }

    private String buildVerificationEmail(String nickname, String verificationLink) {
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
                            <h1>🎵 TikTok</h1>
                        </div>
                        <div class="content">
                            <h2>Xin chào %s!</h2>
                            <p>Cảm ơn bạn đã đăng ký tài khoản TikTok. Vui lòng xác thực email của bạn để bắt đầu sử dụng.</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Xác thực email</a>
                            </p>
                            <p>Hoặc copy link sau vào trình duyệt:</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <p><strong>Lưu ý:</strong> Link này sẽ hết hạn sau 24 giờ.</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 TikTok. All rights reserved.</p>
                            <p>Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(nickname, verificationLink, verificationLink);
    }

    private String buildPasswordResetEmail(String nickname, String resetLink) {
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
                            <h1> Đặt lại mật khẩu</h1>
                        </div>
                        <div class="content">
                            <h2>Xin chào %s!</h2>
                            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản TikTok của bạn.</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Đặt lại mật khẩu</a>
                            </p>
                            <p>Hoặc copy link sau vào trình duyệt:</p>
                            <p style="word-break: break-all; color: #666;">%s</p>
                            <div class="warning">
                                <strong>⚠️ Lưu ý bảo mật:</strong>
                                <ul>
                                    <li>Link này sẽ hết hạn sau 1 giờ</li>
                                    <li>Chỉ sử dụng một lần</li>
                                    <li>Không chia sẻ link này với bất kỳ ai</li>
                                </ul>
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2024 TikTok. All rights reserved.</p>
                            <p><strong>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này và đảm bảo tài khoản của bạn an toàn.</strong></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(nickname, resetLink, resetLink);
    }
}