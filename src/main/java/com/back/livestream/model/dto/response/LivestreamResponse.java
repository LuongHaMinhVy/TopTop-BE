package com.back.livestream.model.dto.response;

import com.back.livestream.model.enums.LivestreamStatus;
import com.back.livestream.model.enums.LivestreamVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LivestreamResponse {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private LivestreamStatus status;
    private LivestreamVisibility visibility;
    private boolean allowChat;
    private boolean allowGifts;
    private String roomName;
    private int viewerCount;
    private long likeCount;
    private long giftCount;
    private String categoryName;
    private HostSummary host;
    private LocalDateTime startedAt;
    private LocalDateTime createdAt;
}
