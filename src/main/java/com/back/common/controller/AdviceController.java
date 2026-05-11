package com.back.common.controller;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.common.utils.Translator;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class AdviceController {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus()).body(
                ApiResponse.builder()
                        .message(errorCode.getMessage())
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(ex.getField())
                                        .message(errorCode.getMessage())
                                        .build()
                        ))
                        .status(errorCode.getStatus().value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {

        List<ErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapToError)
                .toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.validation_failed", "Validation failed"))
                        .data(null)
                        .errors(errors)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    private ErrorResponse mapToError(FieldError error) {
        return ErrorResponse.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.unauthorized", "Unauthorized"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(Translator.toLocale("error.authentication_failed", "Authentication failed"))
                                        .build()
                        ))
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.forbidden", "Forbidden"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(Translator.toLocale("error.access_denied", "You do not have permission to access this resource"))
                                        .build()
                        ))
                        .status(HttpStatus.FORBIDDEN.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingPartException(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.missing_request_part", "Missing request part"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(ex.getRequestPartName())
                                        .message(Translator.toLocale("error.required_part_missing", "Required part is missing"))
                                        .build()
                        ))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.invalid_parameter_type", "Invalid parameter type"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(ex.getName())
                                        .message(Translator.toLocale("error.invalid_value", "Invalid value"))
                                        .build()
                        ))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJsonException(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.invalid_json_format", "Invalid JSON format"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(Translator.toLocale("error.malformed_request_body", "Malformed request body"))
                                        .build()
                        ))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityException(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.database_constraint_violation", "Database constraint violation"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(Translator.toLocale("error.duplicate_or_invalid_data", "Duplicate or invalid data"))
                                        .build()
                        ))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.internal_server_error", "Internal server error"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(Translator.toLocale("error.unexpected_error_occurred", "Unexpected error occurred"))
                                        .build()
                        ))
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiResponse.builder()
                        .message(ErrorCode.FILE_TOO_LARGE.getMessage())
                        .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .message(Translator.toLocale("error.invalid_argument", "Invalid argument"))
                        .data(null)
                        .errors(List.of(
                                ErrorResponse.builder()
                                        .field(null)
                                        .message(ex.getMessage())
                                        .build()
                        ))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}