package com.orang.notificationservice.service;

import com.orang.notificationservice.dto.NotificationResponse;
import com.orang.notificationservice.entity.Notification;
import com.orang.notificationservice.entity.NotificationType;
import com.orang.notificationservice.repository.NotificationRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification saveNotification(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            String groupKey,
            UUID conversationId,
            UUID messageId,
            UUID actorId) {

        if (groupKey != null) {
            Optional<Notification> existing =
                    notificationRepository.findUnreadByGroupKey(userId, groupKey);

            if (existing.isPresent()) {
                Notification notification = existing.get();
                notification.setGroupCount(notification.getGroupCount() + 1);
                notification.setTitle(title);
                notification.setBody(body);
                notification.setMessageId(messageId);
                notification.setActorId(actorId);

                Notification saved = notificationRepository.save(notification);
                log.debug("Updated grouped notification {} for user {}, count now: {}",
                        saved.getId(), userId, saved.getGroupCount());
                return saved;
            }
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .groupKey(groupKey)
                .groupCount(1)
                .conversationId(conversationId)
                .messageId(messageId)
                .actorId(actorId)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Created notification {} for user {}, type: {}",
                saved.getId(), userId, type);
        return saved;
    }

    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        int clampedSize = Math.clamp(size, 1, 50);
        Pageable pageable = PageRequest.of(page, clampedSize);

        return notificationRepository
                .findByUserIdOrdered(userId, pageable)
                .map(this::toResponse);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your notification");
        }

        notificationRepository.delete(notification);
        log.debug("Deleted notification {} for user {}", notificationId, userId);
    }

    @Transactional
    public void clearAll(UUID userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Cleared all notifications for user {}", userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .groupCount(n.getGroupCount())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .conversationId(n.getConversationId())
                .messageId(n.getMessageId())
                .actorId(n.getActorId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}