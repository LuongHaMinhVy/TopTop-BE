package com.back.notification.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.notification.model.dto.response.NotificationResponseDTO;
import com.back.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.common.model.dto.response.Meta;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final INotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDTO> notificationPage = notificationService.getNotifications(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponseDTO>>builder()
                .message(Translator.toLocale("notification.list.success", "Notifications retrieved successfully"))
                .data(notificationPage.getContent())
                .meta(Meta.from(notificationPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }


    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message(Translator.toLocale("notification.count.success", "Unread count retrieved successfully"))
                .data(count)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("notification.read.success", "Notification marked as read"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
