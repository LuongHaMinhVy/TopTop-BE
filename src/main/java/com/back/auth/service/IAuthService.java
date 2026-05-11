package com.back.auth.service;

import com.back.auth.model.dto.request.LoginRequest;
import com.back.auth.model.dto.request.RegisterRequest;
import com.back.auth.model.dto.response.AuthResponse;
import com.back.auth.model.dto.response.AuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService{

    AuthResult login(LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void register(RegisterRequest registerRequest);
    void verifyEmail(String token);
    void resendVerification(String email);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response);
}
