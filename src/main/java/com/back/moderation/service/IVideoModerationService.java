package com.back.moderation.service;

import com.back.moderation.model.dto.response.VideoModerationSummaryResponseDTO;
import com.back.moderation.model.dto.request.ReviewVideoModerationRequestDTO;
import com.back.moderation.model.dto.response.VideoModerationDetailResponseDTO;
import com.back.moderation.model.dto.response.ModerationQueueItemResponseDTO;
import com.back.moderation.model.enums.VideoModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IVideoModerationService {
    void runModeration(Long videoId);
    VideoModerationSummaryResponseDTO getModerationStatus(Long videoId, Long requesterId, boolean isAdmin);
    Page<ModerationQueueItemResponseDTO> getAdminQueue(VideoModerationStatus status, Pageable pageable);
    VideoModerationDetailResponseDTO getAdminDetail(Long videoId);
    VideoModerationSummaryResponseDTO reviewVideo(Long videoId, ReviewVideoModerationRequestDTO request, Long adminUserId);
}
