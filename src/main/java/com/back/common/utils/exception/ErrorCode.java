package com.back.common.utils.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import com.back.common.utils.Translator;

@Getter
public enum ErrorCode {

    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "Email not found"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "Invalid password"),
    WRONG_EMAIL_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "Wrong email or password"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username is already taken"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email is not verified. Please check your inbox to verify your account"),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "Invalid token"),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Token has expired. Please request a new verification email"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "Email is already verified"),
    FILE_UPLOAD_ERROR(HttpStatus.BAD_REQUEST, "Failed to upload file to the storage server"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "File not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, "Genre not found"),
    FILE_IS_REQUIRED(HttpStatus.BAD_REQUEST, "File is required"),
    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "Track not found"),
    TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "Token already used"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is locked. Please contact support for assistance"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later"),
    ROLE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found. Please contact support"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),

    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "Account has been banned. Please contact support for assistance"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Account has been suspended. Please contact support for assistance"),

    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "Invalid refresh token"),

    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "Account is not active. Please contact support for assistance"),

    AUDD_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AudD service error"),

    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "Invalid reset token"),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "Invalid verification token"),

    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Invalid file type."),
    INVALID_VIDEO_FILE_TYPE(HttpStatus.BAD_REQUEST, "Invalid video file type. Only MP4, AVI, MOV, and WMV are supported"),
    INVALID_IMAGE_FILE_TYPE(HttpStatus.BAD_REQUEST, "Invalid image file type. Only JPG, JPEG, PNG, and GIF are supported"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File is too large. Maximum file size is 5MB"),
    INVALID_AUDIO_FILE_TYPE(HttpStatus.BAD_REQUEST, "Invalid audio file type. Only MP3, WAV, and OGG are supported"),

    INVALID_PAGE(HttpStatus.BAD_REQUEST, "Page must not be less than 1"),
    INVALID_SIZE(HttpStatus.BAD_REQUEST, "Size must be greater than 0"),

    GENRE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Genre already exists"),

    OAUTH2_EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "Email not found in OAuth2 provider"),

    COPYRIGHT_DETECTED(HttpStatus.BAD_REQUEST, "Upload rejected: Copyrighted material detected."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getMessage() {
        try {
            return Translator.toLocale("error." + this.name().toLowerCase(), this.message);
        } catch (Exception e) {
            return this.message;
        }
    }
}
