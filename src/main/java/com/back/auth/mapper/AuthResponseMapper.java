package com.back.auth.mapper;

import com.back.auth.model.dto.response.AuthResponse;
import com.back.user.model.dto.response.PrivacySettings;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthResponseMapper {

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    private final IVideoRepository videoRepository;

    public AuthResponse toAuthResponse(User user, String accessToken) {
        UserInfo userInfo = UserInfo.builder()
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
                .videoCount(videoRepository.countByUserIdAndDeletedAtIsNull(user.getId()))
                .verified(user.getVerified())
                .isPrivate(user.getIsPrivate())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .accountType(user.getAccountType() != null ? user.getAccountType().name() : null)
                .websiteUrl(user.getWebsiteUrl())
                .instagramHandle(user.getInstagramHandle())
                .youtubeHandle(user.getYoutubeHandle())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .region(user.getRegion())
                .dateOfBirth(user.getDateOfBirth())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .toList())
                .onboarded(user.getOnboarded())
                .createdAt(user.getCreatedAt())
                .privacySettings(PrivacySettings.builder()
                        .allowComments(user.getAllowComments())
                        .allowDuet(user.getAllowDuet())
                        .allowStitch(user.getAllowStitch())
                        .allowDownload(user.getAllowDownload())
                        .allowMessageFromEveryone(user.getAllowMessageFromEveryone())
                        .build())
                .build();

        return AuthResponse.builder()
                .user(userInfo)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .build();
    }
}
