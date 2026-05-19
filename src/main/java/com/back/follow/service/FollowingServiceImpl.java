package com.back.follow.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.model.dto.FollowingTrayResponseDTO;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.service.IVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class FollowingServiceImpl implements IFollowingService {

    private final IUserRepo userRepo;
    private final IVideoService videoService;
    private final IFollowService followService;

    private User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return null;
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email).orElse(null);
    }

    @Override
    public Page<VideoResponseDTO> getFollowingFeed(Pageable pageable) {
        return videoService.getFollowingFeed(pageable);
    }

    @Override
    public Page<UserInfo> getSuggestions(Pageable pageable) {
        User currentUser = getCurrentUserOrNull();
        Long viewerId = currentUser != null ? currentUser.getId() : null;

        Page<User> suggestedUsers = userRepo.findSuggestedUsersToFollow(viewerId, pageable);

        return suggestedUsers.map(user -> {
            RelationshipStatus rel = currentUser != null 
                    ? followService.getRelationshipStatus(currentUser, user)
                    : RelationshipStatus.builder()
                        .isFollowing(false)
                        .isFollower(false)
                        .isBlocked(false)
                        .isBlockedBy(false)
                        .isFriend(false)
                        .build();
            return UserInfoMapper.buildUserInfo(user, rel);
        });
    }

    @Override
    public FollowingTrayResponseDTO getTray() {
        User currentUser = getCurrentUserOrNull();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return FollowingTrayResponseDTO.builder()
                .liveCount(0)
                .storyCount(0)
                .items(new ArrayList<>())
                .build();
    }
}
