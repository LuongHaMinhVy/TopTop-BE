package com.back.notification.service;

import com.back.notification.model.dto.response.NotificationResponseDTO;
import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface INotificationService {
    void createNotification(User recipient, User actor, Video video, String type, String content);
    Page<NotificationResponseDTO> getNotifications(Pageable pageable);
    void markAsRead(Long notificationId);
    long getUnreadCount();
}

