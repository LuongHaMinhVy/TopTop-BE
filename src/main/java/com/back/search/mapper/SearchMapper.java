package com.back.search.mapper;

import com.back.follow.repo.IFollowRepo;
import com.back.search.model.dto.response.SearchHistoryResponseDTO;
import com.back.search.model.dto.response.SearchUserResponseDTO;
import com.back.search.model.dto.response.SearchVideoResponseDTO;
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
                .build();
    }
}
