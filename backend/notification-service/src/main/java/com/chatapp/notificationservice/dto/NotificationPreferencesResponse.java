package com.chatapp.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPreferencesResponse {

    private UUID conversationId;
    private boolean muted;
    private LocalDateTime mutedUntil;
}
