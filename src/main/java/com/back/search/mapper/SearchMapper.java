package com.back.search.mapper;

import com.back.follow.repo.IFollowRepo;
import com.back.search.model.dto.response.SearchHistoryResponseDTO;
import com.back.search.model.dto.response.SearchUserResponseDTO;
import com.back.search.model.dto.response.SearchVideoResponseDTO;
import com.back.sound.model.dto.response.SoundAuthorResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.dto.response.SoundStatsResponseDTO;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.search.model.entity.SearchHistory;
import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;

public final class SearchMapper {
    private SearchMapper() {
    }

    public static SearchHistoryResponseDTO toHistoryResponse(SearchHistory history) {
        return SearchHistoryResponseDTO.builder()
                .id(history.getId())
                .keyword(history.getKeyword())
                .type(history.getType())
                .sourceType(history.getSourceType())
                .resultTargetId(history.getResultTargetId())
                .searchedAt(history.getSearchedAt())
                .build();
    }

    public static SearchUserResponseDTO toUserResponse(User user, User viewer, IFollowRepo followRepo) {
        boolean followed = viewer != null && !viewer.getId().equals(user.getId())
                && followRepo.existsByFollowerAndFollowing(viewer, user);

        return SearchUserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .verified(user.getVerified())
                .followed(followed)
                .followerCount(user.getFollowersCount())
                .totalLikeCount(user.getTotalLikes())
                .bio(user.getBio())
                .build();
    }

    public static SearchVideoResponseDTO toVideoResponse(Video video, User viewer, IFollowRepo followRepo) {
        return SearchVideoResponseDTO.builder()
                .id(video.getId())
                .videoId(String.valueOf(video.getId()))
                .caption(video.getDescription() != null && !video.getDescription().isBlank()
                        ? video.getDescription()
                        : video.getTitle())
                .coverUrl(video.getThumbnailUrl())
                .videoUrl(video.getFileUrl())
                .likeCount(video.getLikeCount())
                .viewCount(video.getViewCount())
                .commentCount(video.getCommentCount())
                .createdAt(video.getCreatedAt())
                .author(toUserResponse(video.getUser(), viewer, followRepo))
                .sound(toSoundResponse(video.getSound()))
                .build();
    }

    private static SoundResponseDTO toSoundResponse(Sound sound) {
        if (sound == null) return null;
        long usageCount = sound.getUsageCount() == null ? 0L : sound.getUsageCount();
        long savedCount = sound.getSavedCount() == null ? 0L : sound.getSavedCount();

        return SoundResponseDTO.builder()
                .id(sound.getId())
                .title(sound.getTitle())
                .artistName(sound.getArtistName())
                .audioUrl(sound.getAudioUrl())
                .coverUrl(sound.getCoverUrl())
                .durationSeconds(sound.getDurationSeconds())
                .type(sound.getType().name())
                .originalSound(sound.getType() == SoundType.ORIGINAL)
                .owner(toSoundAuthorResponse(sound.getOwner()))
                .stats(SoundStatsResponseDTO.builder()
                        .soundId(sound.getId())
                        .usageCount(usageCount)
                        .videoCount(usageCount)
                        .savedCount(savedCount)
                        .isSaved(false)
                        .build())
                .isSaved(false)
                .isPublic(sound.getIsPublic())
                .isActive(sound.getIsActive())
                .createdAt(sound.getCreatedAt())
                .build();
    }

    private static SoundAuthorResponseDTO toSoundAuthorResponse(User user) {
        if (user == null) return null;
        return SoundAuthorResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.getVerified())
                .build();
    }
}
