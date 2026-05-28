package com.back.user.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.service.IFollowService;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.request.AccountStatusActionRequestDTO;
import com.back.user.model.dto.request.AccountStatusConfirmRequestDTO;
import com.back.user.model.dto.request.AccountStatusOtpRequestDTO;
import com.back.user.model.dto.request.ChangePasswordRequestDTO;
import com.back.user.model.dto.request.ContentFilterTagRequestDTO;
import com.back.user.model.dto.request.UpdatePrivacySettingsRequestDTO;
import com.back.user.model.dto.response.ContentFilterTagResponseDTO;
import com.back.user.model.dto.response.MentionSuggestionResponseDTO;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.model.entity.UserContentFilterTag;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IUserContentFilterTagRepo;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.common.service.R2StorageService;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    private final IUserRepo userRepo;
    private final IFollowService followService;
    private final R2StorageService storageService;
    private final UserInfoMapper userInfoMapper;
    private final IUserContentFilterTagRepo contentFilterTagRepo;
    private final PasswordEncoder passwordEncoder;
    private final AccountStatusOtpService accountStatusOtpService;

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

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
        return user;
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

        return userInfoMapper.buildUserInfo(user);
    }

    @Override
    public UserInfo getUserProfile(String username) {
        log.info("Fetching UserInfo for profile: {}", username);
        User targetUser = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User currentUser = getCurrentUser();
        RelationshipStatus relationship = followService.getRelationshipStatus(currentUser, targetUser);
        if (relationship != null && Boolean.TRUE.equals(relationship.getIsBlockedBy())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        return userInfoMapper.buildUserInfo(targetUser, relationship);
    }

    @Override
    public List<MentionSuggestionResponseDTO> getMentionSuggestions(String keyword) {
        List<User> users;
        if (keyword == null || keyword.trim().isEmpty()) {
            users = userRepo.findRecentPublicUsers(PageRequest.of(0, 10));
        } else {
            String q = keyword.trim();
            users = userRepo.findPublicMentionSuggestions(q, PageRequest.of(0, 10));
        }

        return users.stream().map(u -> MentionSuggestionResponseDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .displayName(u.getNickname() != null ? u.getNickname() : u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .verified(u.getVerified())
                .build()).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "userInfo", key = "#result.email")
    public UserInfo updateProfile(com.back.user.model.dto.request.UpdateProfileRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate username uniqueness if changed
        if (!currentUser.getUsername().equals(request.getUsername())) {
            if (isReservedUserUsername(request.getUsername())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            if (userRepo.existsByUsername(request.getUsername())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            currentUser.setUsername(request.getUsername());
        }

        currentUser.setNickname(request.getNickname());
        currentUser.setBio(request.getBio());
        
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            currentUser.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userRepo.save(currentUser);
        log.info("Successfully updated profile for user: {}", updatedUser.getEmail());

        return userInfoMapper.buildUserInfo(updatedUser);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "userInfo", key = "#result.email")
    public UserInfo updatePrivacySettings(UpdatePrivacySettingsRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getIsPrivate() != null) {
            currentUser.setIsPrivate(request.getIsPrivate());
        }
        if (request.getAllowComments() != null) {
            currentUser.setAllowComments(request.getAllowComments());
        }
        if (request.getAllowMessageFromEveryone() != null) {
            currentUser.setAllowMessageFromEveryone(request.getAllowMessageFromEveryone());
        }

        return userInfoMapper.buildUserInfo(userRepo.save(currentUser));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(currentUser);
    }

    @Override
    @Transactional
    @Deprecated
    public void updateAccountStatus(AccountStatusActionRequestDTO request) {
        AccountStatusOtpRequestDTO otpRequest = new AccountStatusOtpRequestDTO();
        otpRequest.setAction(request.getAction());
        sendAccountStatusOtp(otpRequest);
    }

    @Override
    @Transactional
    public void sendAccountStatusOtp(AccountStatusOtpRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        accountStatusOtpService.sendOtp(currentUser, request.getAction());
    }

    @Override
    @Transactional
    public void confirmAccountStatus(AccountStatusConfirmRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        accountStatusOtpService.verifyOtp(currentUser, request.getAction(), request.getOtp());

        String action = request.getAction().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        if ("DEACTIVATE".equals(action)) {
            currentUser.setStatus(UserStatus.SUSPENDED);
            currentUser.setDeletedAt(now);
            currentUser.setDeletionScheduledAt(null);
        } else if ("DELETE".equals(action)) {
            currentUser.setStatus(UserStatus.BANNED);
            currentUser.setDeletedAt(now);
            currentUser.setDeletionScheduledAt(now.plusDays(30));
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        userRepo.save(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentFilterTagResponseDTO> getContentFilterTags() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return contentFilterTagRepo.findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::mapContentFilterTag)
                .toList();
    }

    @Override
    @Transactional
    public ContentFilterTagResponseDTO addContentFilterTag(ContentFilterTagRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String normalizedTag = normalizeTag(request.getTag());
        UserContentFilterTag tag = contentFilterTagRepo.findByUserAndTag(currentUser, normalizedTag)
                .orElseGet(() -> UserContentFilterTag.builder()
                        .user(currentUser)
                        .tag(normalizedTag)
                        .build());
        if (request.getSampleThumbnailUrl() != null && !request.getSampleThumbnailUrl().isBlank()) {
            tag.setSampleThumbnailUrl(request.getSampleThumbnailUrl().trim());
        }

        return mapContentFilterTag(contentFilterTagRepo.save(tag));
    }

    @Override
    @Transactional
    public void deleteContentFilterTag(String tag) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        contentFilterTagRepo.deleteByUserAndTag(currentUser, normalizeTag(tag));
    }

    @Override
    public String uploadAvatar(MultipartFile file) throws java.io.IOException {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_REQUIRED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        
        String key = "avatars/" + java.util.UUID.randomUUID() + ext;
        return storageService.uploadFile(file, key);
    }

    private boolean isReservedUserUsername(String username) {
        return username != null && "admin".equalsIgnoreCase(username.trim());
    }

    private String normalizeTag(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.length() > 80) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private ContentFilterTagResponseDTO mapContentFilterTag(UserContentFilterTag tag) {
        return ContentFilterTagResponseDTO.builder()
                .id(tag.getId())
                .tag(tag.getTag())
                .sampleThumbnailUrl(tag.getSampleThumbnailUrl())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
