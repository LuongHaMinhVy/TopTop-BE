package com.back.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfo {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String bio;

    private String avatarUrl;

    private String coverUrl;

    private Long followersCount;

    private Long followingCount;

    private Long totalLikes;

    private Long videoCount;

    private Boolean verified;

    private Boolean isPrivate;

    private String status;

    private String accountType;

    private String websiteUrl;

    private String instagramHandle;

    private String youtubeHandle;

    private String gender;

    private String region;

    private LocalDate dateOfBirth;

    private PrivacySettings privacySettings;
    private RelationshipStatus relationship;

    private List<String> roles;
    private Boolean onboarded;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

}
