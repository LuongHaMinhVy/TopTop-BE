package com.back.follow.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.service.IVideoService;
import com.back.follow.repo.IFollowRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements IFriendService {

    private final IUserRepo userRepo;
    private final IFollowRepo followRepo;
    private final IVideoService videoService;
    private final IFollowService followService;
    private final UserInfoMapper userInfoMapper;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    @Override
    public Page<VideoResponseDTO> getFriendsFeed(Pageable pageable) {
        return videoService.getFriendsFeed(pageable);
    }

    @Override
    public Page<UserInfo> getSuggestions(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<User> suggestedUsers = userRepo.findSuggestedFriends(currentUser.getId(), pageable);

        return suggestedUsers.map(user -> {
            RelationshipStatus rel = followService.getRelationshipStatus(currentUser, user);
            return userInfoMapper.buildUserInfo(user, rel);
        });
    }

    @Override
    public long countFriends() {
        User currentUser = getCurrentUser();
        return followRepo.countFriends(currentUser.getId());
    }
}
