package com.back.follow.service;

import com.back.user.model.dto.response.UserInfo;
import com.back.video.model.dto.request.VideoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFriendService {
    Page<VideoResponseDTO> getFriendsFeed(Pageable pageable);
    Page<UserInfo> getSuggestions(Pageable pageable);
    long countFriends();
}
