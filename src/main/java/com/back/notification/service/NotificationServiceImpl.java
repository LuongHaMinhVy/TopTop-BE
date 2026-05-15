package com.back.notification.service;

import com.back.notification.model.dto.response.NotificationResponseDTO;
import com.back.notification.model.entity.Notification;
import com.back.notification.repo.INotificationRepo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {
    private final INotificationRepo notificationRepo;
    private final IUserRepo userRepo;

    @Override
    @Transactional
    public void createNotification(User recipient, User actor, Video video, String type, String content) {
        if (recipient.getId().equals(actor.getId())) return; 

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .video(video)
                .type(type)
                .content(content)
                .isRead(false)
                .build();
        notificationRepo.save(notification);
    }

    @Override
    public Page<NotificationResponseDTO> getNotifications(Pageable pageable) {
        User user = getCurrentUser();
        return notificationRepo.findByRecipientWithDetails(user, pageable)
                .map(this::mapToResponseDTO);
    }


    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }

    @Override
    public long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepo.countByRecipientAndIsReadFalse(user);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    private NotificationResponseDTO mapToResponseDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .content(notification.getContent())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .actorId(notification.getActor().getId())
                .actorUsername(notification.getActor().getUsername())
                .actorAvatarUrl(notification.getActor().getAvatarUrl())
                .videoId(notification.getVideo() != null ? notification.getVideo().getId() : null)
                .videoThumbnailUrl(notification.getVideo() != null ? notification.getVideo().getThumbnailUrl() : null)
                .build();
    }
}
