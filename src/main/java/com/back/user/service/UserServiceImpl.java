package com.back.user.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.service.IFollowService;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.MentionSuggestionResponseDTO;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    private final IUserRepo userRepo;
    private final IFollowService followService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return null;
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    @Override
    public UserInfo getUserInfo(HttpServletRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return getCachedUserInfo(user.getEmail());
    }

    @Cacheable(value = "userInfo", key = "#email")
    public UserInfo getCachedUserInfo(String email) {
        log.info("Fetching UserInfo from DB for: {}", email);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        return UserInfoMapper.buildUserInfo(user);
    }

    @Override
    public UserInfo getUserProfile(String username) {
        log.info("Fetching UserInfo for profile: {}", username);
        User targetUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User currentUser = getCurrentUser();
        RelationshipStatus relationship = followService.getRelationshipStatus(currentUser, targetUser);

        return UserInfoMapper.buildUserInfo(targetUser, relationship);
    }

    @Override
    public List<MentionSuggestionResponseDTO> getMentionSuggestions(String keyword) {
        List<User> users;
        if (keyword == null || keyword.trim().isEmpty()) {
            users = userRepo.findTop10ByOrderByCreatedAtDesc();
        } else {
            String q = keyword.trim();
            users = userRepo.findTop10ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(q, q);
        }

        return users.stream().map(u -> MentionSuggestionResponseDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .displayName(u.getNickname() != null ? u.getNickname() : u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .verified(u.getVerified())
                .build()).collect(java.util.stream.Collectors.toList());
    }
}
