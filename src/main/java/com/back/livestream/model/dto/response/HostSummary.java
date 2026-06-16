package com.back.livestream.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HostSummary {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isFollowing;
}
