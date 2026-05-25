package com.back.video.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.back.video.model.dto.response.VideoRepostUserResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String thumbnailUrl;
    private Integer duration;
    private String category;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long saveCount;
    private Long shareCount;
    private Long userId;
    private String username;
    private String userNickname;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
    private Boolean isSaved;
    private Boolean isLiked;
    private Boolean isReposted;
    private List<VideoRepostUserResponseDTO> repostedBy;
    private Boolean isFollowingAuthor;
    private Boolean allowComments;
    private String visibility;
    private Boolean deleted;
    private Boolean unavailable;
    private SoundResponseDTO sound;
    private String moderationStatus;
    private LocalDateTime moderationCheckedAt;
    private String moderationReasonCode;
    private String moderationReasonMessage;
    private String musicCopyrightStatus;
    private LocalDateTime musicCopyrightCheckedAt;
    private String musicCopyrightReasonCode;
    private String musicCopyrightReasonMessage;
}
