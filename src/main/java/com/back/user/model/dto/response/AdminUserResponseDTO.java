package com.back.user.model.dto.response;

import com.back.user.model.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponseDTO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Long followersCount;
    private Long followingCount;
    private Long totalLikes;
    private Boolean verified;
    private Boolean isPrivate;
    private UserStatus status;
    private String statusReason;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime deletionScheduledAt;
}
