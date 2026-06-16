package com.back.user.service;

import com.back.common.service.emailservice.EmailService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AccountStatusOtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESEND_SECONDS = 30;
    private static final int EXPIRE_MINUTES = 10;

    private final EmailService emailService;
    private final Map<String, OtpData> otps = new ConcurrentHashMap<>();

    public void sendOtp(User user, String action) {
        String normalizedAction = normalizeAction(action);
        String key = buildKey(user, normalizedAction);
        OtpData existing = otps.get(key);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && existing.lastSentAt.plusSeconds(RESEND_SECONDS).isAfter(now)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Please wait before requesting another OTP");
        }

        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        otps.put(key, new OtpData(otp, now, now.plusMinutes(EXPIRE_MINUTES)));
        emailService.sendHtmlEmail(
                user.getEmail(),
                buildSubject(normalizedAction),
                buildEmail(user.getNickname(), normalizedAction, otp)
        );
    }

    public void verifyOtp(User user, String action, String otp) {
        String normalizedAction = normalizeAction(action);
        String key = buildKey(user, normalizedAction);
        OtpData data = otps.get(key);
        if (data == null || data.expiresAt.isBefore(LocalDateTime.now()) || !data.otp.equals(otp)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Invalid or expired OTP");
        }
        otps.remove(key);
    }

    private String normalizeAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase();
        if (!"DEACTIVATE".equals(normalized) && !"DELETE".equals(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String buildKey(User user, String action) {
        return user.getId() + ":" + action;
    }

    private String buildSubject(String action) {
        return "DELETE".equals(action) ? "Confirm account deletion" : "Confirm account deactivation";
    }

    private String buildEmail(String nickname, String action, String otp) {
        String actionText = "DELETE".equals(action) ? "delete your TopTop account" : "deactivate your TopTop account";
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;color:#222;line-height:1.5">
                  <div style="max-width:560px;margin:0 auto;padding:24px">
                    <h2>Confirm account action</h2>
                    <p>Hello %s,</p>
                    <p>Use this 6-digit code to %s:</p>
                    <div style="font-size:32px;font-weight:700;letter-spacing:8px;margin:24px 0">%s</div>
                    <p>This code expires in 10 minutes. If you did not request this, ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(nickname, actionText, otp);
    }

    private record OtpData(String otp, LocalDateTime lastSentAt, LocalDateTime expiresAt) {}
}
