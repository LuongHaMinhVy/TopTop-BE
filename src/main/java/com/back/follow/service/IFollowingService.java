package com.back.follow.service;

import com.back.follow.model.dto.FollowingTrayResponseDTO;
import com.back.user.model.dto.response.UserInfo;
import com.back.video.model.dto.request.VideoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFollowingService {
    Page<VideoResponseDTO> getFollowingFeed(Pageable pageable);
    Page<UserInfo> getSuggestions(Pageable pageable);
    FollowingTrayResponseDTO getTray();
}
