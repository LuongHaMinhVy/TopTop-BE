package com.back.livestream.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.livestream.model.dto.response.AdminLivestreamResponseDTO;
import com.back.livestream.model.dto.response.HostSummary;
import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.enums.LivestreamStatus;
import com.back.livestream.repo.ILivestreamRepo;
import com.back.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminLivestreamServiceImpl implements IAdminLivestreamService {

    private final ILivestreamRepo livestreamRepo;
    private final LiveKitTokenService liveKitTokenService;
    private final LivestreamEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminLivestreamResponseDTO> listLivestreams(
            String keyword,
            LivestreamStatus status,
            Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return livestreamRepo.adminFindLivestreams(normalizedKeyword, status, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public AdminLivestreamResponseDTO endLivestream(Long livestreamId) {
        Livestream livestream = livestreamRepo.findById(livestreamId)
                .orElseThrow(() -> new AppException(ErrorCode.LIVESTREAM_NOT_FOUND));

        if (livestream.getStatus() != LivestreamStatus.ENDED
                && livestream.getStatus() != LivestreamStatus.CANCELLED) {
            liveKitTokenService.deleteRoom(livestream.getRoomName());
            livestream.setStatus(LivestreamStatus.ENDED);
            livestream.setEndedAt(LocalDateTime.now());
            livestream.setViewerCount(0);
            livestream = livestreamRepo.save(livestream);
            eventPublisher.publishStreamEnded(livestreamId);
        }

        return toDto(livestream);
    }

    private AdminLivestreamResponseDTO toDto(Livestream livestream) {
        User host = livestream.getHost();
        return AdminLivestreamResponseDTO.builder()
                .id(livestream.getId())
                .title(livestream.getTitle())
                .description(livestream.getDescription())
                .thumbnailUrl(livestream.getThumbnailUrl())
                .status(livestream.getStatus())
                .visibility(livestream.getVisibility())
                .allowChat(livestream.isAllowChat())
                .allowGifts(livestream.isAllowGifts())
                .roomName(livestream.getRoomName())
                .viewerCount(livestream.getViewerCount())
                .peakViewerCount(livestream.getPeakViewerCount())
                .likeCount(livestream.getLikeCount())
                .giftCount(livestream.getGiftCount())
                .categoryName(livestream.getCategory() != null ? livestream.getCategory().getName() : null)
                .host(HostSummary.builder()
                        .id(host.getId())
                        .username(host.getUsername())
                        .displayName(resolveDisplayName(host))
                        .avatarUrl(host.getAvatarUrl())
                        .isFollowing(false)
                        .build())
                .startedAt(livestream.getStartedAt())
                .endedAt(livestream.getEndedAt())
                .createdAt(livestream.getCreatedAt())
                .build();
    }

    private String resolveDisplayName(User user) {
        String nickname = user.getNickname();
        return nickname != null && !nickname.isBlank() ? nickname : user.getUsername();
    }
}
