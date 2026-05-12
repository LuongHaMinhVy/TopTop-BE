package com.back.user.mapper;

import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserInfoMapper{

    public static UserInfo buildUserInfo(User user) {
        return buildUserInfo(user, null);
    }

    public static UserInfo buildUserInfo(User user, RelationshipStatus relationship) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return UserInfo.builder()
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
                .videoCount(user.getVideoCount())
                .verified(user.getVerified())
                .isPrivate(user.getIsPrivate())
                .status(user.getStatus().name())
                .accountType(user.getAccountType().name())
                .websiteUrl(user.getWebsiteUrl())
                .instagramHandle(user.getInstagramHandle())
                .youtubeHandle(user.getYoutubeHandle())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .region(user.getRegion())
                .dateOfBirth(user.getDateOfBirth())
                .roles(roles)
                .relationship(relationship)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
