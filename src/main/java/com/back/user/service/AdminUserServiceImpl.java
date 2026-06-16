package com.back.user.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.dto.request.AdminUpdateUserStatusRequestDTO;
import com.back.user.model.dto.response.AdminUserResponseDTO;
import com.back.user.model.entity.User;
import com.back.user.model.enums.UserStatus;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final IUserRepo userRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> listUsers(String keyword, UserStatus status, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<User> users = userRepo.adminFindUsers(normalizedKeyword, status, pageable);
        return users.map(this::toDto);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserStatus(Long userId, AdminUpdateUserStatusRequestDTO request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String reason = request.getReason() == null ? null : request.getReason().trim();
        if (request.getStatus() != UserStatus.ACTIVE && (reason == null || reason.isBlank())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "reason", "Reason is required when locking an account");
        }
        user.setStatus(request.getStatus());
        if (request.getStatus() == UserStatus.ACTIVE) {
            user.setStatusReason(null);
            user.setDeletedAt(null);
            user.setDeletionScheduledAt(null);
        } else {
            user.setStatusReason(reason);
        }
        return toDto(userRepo.save(user));
    }

    private AdminUserResponseDTO toDto(User user) {
        return AdminUserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .totalLikes(user.getTotalLikes())
                .verified(user.getVerified())
                .isPrivate(user.getIsPrivate())
                .status(user.getStatus())
                .statusReason(user.getStatusReason())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .deletionScheduledAt(user.getDeletionScheduledAt())
                .build();
    }
}
