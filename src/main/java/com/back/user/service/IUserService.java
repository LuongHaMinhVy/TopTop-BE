package com.back.user.service;

import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.dto.request.UpdateProfileRequestDTO;

import java.util.List;

import com.back.user.model.dto.response.MentionSuggestionResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface IUserService {
    UserInfo getUserInfo(HttpServletRequest request);
    UserInfo getUserProfile(String username);
    List<MentionSuggestionResponseDTO> getMentionSuggestions(String keyword);
    UserInfo updateProfile(UpdateProfileRequestDTO request);
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
}

