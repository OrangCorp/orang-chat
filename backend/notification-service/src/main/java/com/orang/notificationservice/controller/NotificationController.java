package com.orang.notificationservice.controller;

import com.orang.notificationservice.dto.NotificationResponse;
import com.orang.notificationservice.dto.UnreadCountResponse;
import com.orang.notificationservice.service.NotificationPersistenceService;
import com.orang.notificationservice.service.UnreadCountBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationPersistenceService notificationService;
    private final UnreadCountBroadcastService broadcastService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String userId) {

        return ResponseEntity.ok(
                notificationService.getNotifications(UUID.fromString(userId), page, size)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal String userId) {

        long count = notificationService.getUnreadCount(UUID.fromString(userId));
        return ResponseEntity.ok(
                UnreadCountResponse.builder().unreadCount(count).build()
        );
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);
        NotificationResponse response = notificationService.markAsRead(notificationId, userUUID);
        broadcastService.broadcast(userUUID);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);
        notificationService.markAllAsRead(userUUID);
        broadcastService.broadcast(userUUID);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);
        notificationService.deleteNotification(notificationId, userUUID);
        broadcastService.broadcast(userUUID);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll(
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);
        notificationService.clearAll(userUUID);
        broadcastService.broadcast(userUUID);
        return ResponseEntity.noContent().build();
    }
}