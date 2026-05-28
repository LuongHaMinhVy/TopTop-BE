package com.back.user.service;

import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.dto.response.ContentFilterTagResponseDTO;
import com.back.user.model.dto.request.AccountStatusActionRequestDTO;
import com.back.user.model.dto.request.AccountStatusConfirmRequestDTO;
import com.back.user.model.dto.request.AccountStatusOtpRequestDTO;
import com.back.user.model.dto.request.ChangePasswordRequestDTO;
import com.back.user.model.dto.request.ContentFilterTagRequestDTO;
import com.back.user.model.dto.request.UpdateProfileRequestDTO;
import com.back.user.model.dto.request.UpdatePrivacySettingsRequestDTO;

import java.util.List;

import com.back.user.model.dto.response.MentionSuggestionResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface IUserService {
    UserInfo getUserInfo(HttpServletRequest request);
    UserInfo getUserProfile(String username);
    List<MentionSuggestionResponseDTO> getMentionSuggestions(String keyword);
    UserInfo updateProfile(UpdateProfileRequestDTO request);
    UserInfo updatePrivacySettings(UpdatePrivacySettingsRequestDTO request);
    void changePassword(ChangePasswordRequestDTO request);
    void sendAccountStatusOtp(AccountStatusOtpRequestDTO request);
    void confirmAccountStatus(AccountStatusConfirmRequestDTO request);
    void updateAccountStatus(AccountStatusActionRequestDTO request);
    List<ContentFilterTagResponseDTO> getContentFilterTags();
    ContentFilterTagResponseDTO addContentFilterTag(ContentFilterTagRequestDTO request);
    void deleteContentFilterTag(String tag);
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
}
