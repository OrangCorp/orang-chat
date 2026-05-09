package com.orang.notificationservice.dto;

import com.orang.notificationservice.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private int groupCount;
    private boolean read;
    private LocalDateTime readAt;
    private UUID conversationId;
    private UUID messageId;
    private UUID actorId;
    private LocalDateTime createdAt;
}