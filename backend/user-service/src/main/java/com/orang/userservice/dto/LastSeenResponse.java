package com.orang.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastSeenResponse {

    private String userId;
    private String lastSeenAt;    // ISO-8601 format: "2026-03-28T14:30:00Z"
    private boolean isOnline;

    public static LastSeenResponse from(String userId, Long lastActivityTime, boolean isOnline) {
        String lastSeenAt = null;

        if (lastActivityTime != null) {
            lastSeenAt = Instant.ofEpochSecond(lastActivityTime).toString();
        }

        return LastSeenResponse.builder()
                .userId(userId)
                .lastSeenAt(lastSeenAt)
                .isOnline(isOnline)
                .build();
    }
}