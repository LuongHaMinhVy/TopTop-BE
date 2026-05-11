package com.back.user.service;

import com.back.auth.security.jwt.JwtService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.repo.IRoleRepo;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService{
    private final IUserRepo userRepo;
    private final IRoleRepo roleRepo;
    private final JwtService jwtService;

    @Override
    public UserInfo getUserInfo(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email;

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        if (email == null || email.equals("anonymousUser")) {
            throw new AppException(ErrorCode.EMAIL_NOT_FOUND);
        }

        return getCachedUserInfo(email);
    }

    @org.springframework.cache.annotation.Cacheable(value = "userInfo", key = "#email")
    public UserInfo getCachedUserInfo(String email) {
        log.info("Fetching UserInfo from DB for: {}", email);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        return UserInfoMapper.buildUserInfo(user);
    }
}
