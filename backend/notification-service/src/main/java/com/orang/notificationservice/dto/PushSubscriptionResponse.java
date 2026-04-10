package com.orang.notificationservice.dto;

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
public class PushSubscriptionResponse {

    private UUID id;
    private String endpoint;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String userAgent;
}
