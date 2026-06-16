package com.back.block.service;

import com.back.block.model.entity.UserBlock;
import com.back.block.repo.IUserBlockRepo;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.repo.IFollowRepo;
import com.back.user.mapper.UserInfoMapper;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements IUserBlockService {

    private final IUserBlockRepo userBlockRepo;
    private final IFollowRepo followRepo;
    private final IUserRepo userRepo;
    private final UserInfoMapper userInfoMapper;

    @Override
    @Transactional
    public void blockUser(String username) {
        User currentUser = getCurrentUserOrThrow();
        User targetUser = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        if (!userBlockRepo.existsByBlockerAndBlocked(currentUser, targetUser)) {
            userBlockRepo.save(UserBlock.builder()
                    .blocker(currentUser)
                    .blocked(targetUser)
                    .build());
        }

        removeFollowIfExists(currentUser, targetUser);
        removeFollowIfExists(targetUser, currentUser);
    }

    @Override
    @Transactional
    public void unblockUser(String username) {
        User currentUser = getCurrentUserOrThrow();
        User targetUser = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userBlockRepo.findByBlockerAndBlocked(currentUser, targetUser)
                .ifPresent(userBlockRepo::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserInfo> getBlockedUsers(Pageable pageable) {
        User currentUser = getCurrentUserOrThrow();
        return userBlockRepo.findPublicBlockedByBlocker(currentUser, pageable)
                .map(block -> userInfoMapper.buildUserInfo(block.getBlocked()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(User first, User second) {
        if (first == null || second == null || first.getId().equals(second.getId())) {
            return false;
        }

        return userBlockRepo.existsByBlockerAndBlocked(first, second)
                || userBlockRepo.existsByBlockerAndBlocked(second, first);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertNotBlockedEitherWay(User first, User second) {
        if (isBlockedEitherWay(first, second)) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }
    }

    private void removeFollowIfExists(User follower, User following) {
        followRepo.findByFollowerAndFollowing(follower, following)
                .ifPresent(follow -> {
                    followRepo.delete(follow);
                    follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
                    following.setFollowersCount(Math.max(0, following.getFollowersCount() - 1));
                    userRepo.save(follower);
                    userRepo.save(following);
                });
    }

    private User getCurrentUserOrThrow() {
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
}
