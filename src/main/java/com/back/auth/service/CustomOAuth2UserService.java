package com.back.auth.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.*;
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
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email     = (String) attributes.get("email");
        String name      = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");

        if (email == null) {
            throw new AppException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
        }

        userRepo.findByEmail(email).orElseGet(() -> createGoogleUser(email, name, avatarUrl));

        return oAuth2User;
    }

    private User createGoogleUser(String email, String name, String avatarUrl) {
        Role userRole = roleRepo.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String username = baseUsername;
        int suffix = 1;
        while (userRepo.existsByUsername(username)) {
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
                .videoCount(0L)
                .isPrivate(false)
                .accountType(AccountType.PERSONAL)
                .allowComments(true)
                .allowDuet(true)
                .allowStitch(true)
                .allowDownload(true)
                .allowMessageFromEveryone(false)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepo.save(newUser);
        log.info("New Google user registered: {}", email);
        return saved;
    }
}