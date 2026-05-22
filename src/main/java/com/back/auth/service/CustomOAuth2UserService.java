package com.back.auth.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.*;
import com.back.user.model.enums.AccountType;
import com.back.user.model.enums.Gender;
import com.back.user.model.enums.RoleName;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IRoleRepo;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUserRepo userRepo;
    private final IRoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("google".equals(registrationId)) {
            processGoogleUser(oAuth2User);
        } else if ("facebook".equals(registrationId)) {
            processFacebookUser(oAuth2User);
        }

        return oAuth2User;
    }

    private void processGoogleUser(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");

        validateEmail(email, "google");
        updateOrCreateUser(email, name, avatarUrl, "google");
    }

    private void processFacebookUser(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = null;

        if (attributes.containsKey("picture")) {
            Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
            if (pictureObj.containsKey("data")) {
                Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                avatarUrl = (String) dataObj.get("url");
            }
        }

        validateEmail(email, "facebook");
        updateOrCreateUser(email, name, avatarUrl, "facebook");
    }

    private void updateOrCreateUser(String email, String name, String avatarUrl, String provider) {
        userRepo.findByEmail(email).ifPresentOrElse(
            user -> updateExistingUser(user, name, avatarUrl, provider),
            () -> createNewOAuth2User(email, name, avatarUrl, provider)
        );
    }

    private void updateExistingUser(User user, String name, String avatarUrl, String provider) {
        boolean changed = false;
        if (Boolean.FALSE.equals(user.getOnboarded())) {
            user.setOnboarded(true);
            changed = true;
        }
        if (name != null && !name.equals(user.getNickname())) {
            user.setNickname(name);
            changed = true;
        }
        if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }
        if (changed) {
            userRepo.save(user);
            log.info("Updated existing OAuth2 user from {}: {}", provider, user.getEmail());
        }
    }

    private void createNewOAuth2User(String email, String name, String avatarUrl, String provider) {
        Role userRole = roleRepo.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String username = baseUsername;
        int suffix = 1;
        while (isReservedUserUsername(username) || userRepo.existsByUsername(username)) {
            username = baseUsername + "_" + suffix++;
        }

        User newUser = User.builder()
                .username(username)
                .nickname(name != null ? name : username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .verified(true)
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .avatarUrl(avatarUrl)
                .followersCount(0L)
                .followingCount(0L)
                .gender(Gender.OTHER)
                .totalLikes(0L)
                .isPrivate(false)
                .accountType(AccountType.PERSONAL)
                .allowComments(true)
                .allowDuet(true)
                .allowStitch(true)
                .allowDownload(true)
                .allowMessageFromEveryone(false)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .onboarded(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRepo.save(newUser);
        log.info("Registered new OAuth2 user from {}: {}", provider, email);
    }

    private void validateEmail(String email, String provider) {
        if (email == null) {
            log.error("Email not found from OAuth2 provider: {}", provider);
            throw new AppException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
        }
    }

    private boolean isReservedUserUsername(String username) {
        return username != null && "admin".equalsIgnoreCase(username.trim());
    }
}
