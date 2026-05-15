package com.back.follow.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.block.repo.IUserBlockRepo;
import com.back.follow.model.entity.Follow;
import com.back.follow.repo.IFollowRepo;
import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {

    private final IFollowRepo followRepo;
    private final IUserRepo userRepo;
    private final IUserBlockRepo userBlockRepo;

    private User getCurrentUser() {
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

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    @Override
    @Transactional
    public void followUser(String targetUsername) {
        User currentUser = getCurrentUser();
        if (currentUser == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        User targetUser = userRepo.findByUsername(targetUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (currentUser.equals(targetUser)) {
            throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        if (userBlockRepo.existsByBlockerAndBlocked(currentUser, targetUser)
                || userBlockRepo.existsByBlockerAndBlocked(targetUser, currentUser)) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        if (followRepo.existsByFollowerAndFollowing(currentUser, targetUser)) {
            return;
        }

        Follow follow = Follow.builder()
                .follower(currentUser)
                .following(targetUser)
                .build();
        followRepo.save(follow);

        currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);
        targetUser.setFollowersCount(targetUser.getFollowersCount() + 1);
        userRepo.save(currentUser);
        userRepo.save(targetUser);
    }

    @Override
    @Transactional
    public void unfollowUser(String targetUsername) {
        User currentUser = getCurrentUser();
        if (currentUser == null) throw new AppException(ErrorCode.UNAUTHORIZED);

        User targetUser = userRepo.findByUsername(targetUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        followRepo.findByFollowerAndFollowing(currentUser, targetUser)
                .ifPresent(follow -> {
                    followRepo.delete(follow);
                    currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));
                    targetUser.setFollowersCount(Math.max(0, targetUser.getFollowersCount() - 1));
                    userRepo.save(currentUser);
                    userRepo.save(targetUser);
                });
    }

    @Override
    public RelationshipStatus getRelationshipStatus(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null || currentUser.equals(targetUser)) {
            return null;
        }

        boolean isFollowing = followRepo.existsByFollowerAndFollowing(currentUser, targetUser);
        boolean isFollower = followRepo.existsByFollowerAndFollowing(targetUser, currentUser);
        boolean isBlocked = userBlockRepo.existsByBlockerAndBlocked(currentUser, targetUser);
        boolean isBlockedBy = userBlockRepo.existsByBlockerAndBlocked(targetUser, currentUser);

        return RelationshipStatus.builder()
                .isFollowing(isFollowing)
                .isFollower(isFollower)
                .isBlocked(isBlocked)
                .isBlockedBy(isBlockedBy)
                .isFriend(isFollowing && isFollower)
                .build();
    }

    @Override
    public Page<UserInfo> getFollowingList(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Page.empty();

        return followRepo.findByFollower(currentUser, pageable)
                .map(follow -> UserInfoMapper.buildUserInfo(follow.getFollowing()));
    }
}
