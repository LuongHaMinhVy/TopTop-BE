package com.back.notification.service;

import com.back.notification.model.dto.response.NotificationResponseDTO;
import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;
import java.util.List;

public interface INotificationService {
    void createNotification(User recipient, User actor, Video video, String type, String content);
    List<NotificationResponseDTO> getNotifications();
    void markAsRead(Long notificationId);
    long getUnreadCount();
}
