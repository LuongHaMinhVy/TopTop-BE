package com.back.user.service;

import com.back.user.model.dto.request.AdminUpdateUserStatusRequestDTO;
import com.back.user.model.dto.response.AdminUserResponseDTO;
import com.back.user.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminUserService {
    Page<AdminUserResponseDTO> listUsers(String keyword, UserStatus status, Pageable pageable);
    AdminUserResponseDTO updateUserStatus(Long userId, AdminUpdateUserStatusRequestDTO request);
}
