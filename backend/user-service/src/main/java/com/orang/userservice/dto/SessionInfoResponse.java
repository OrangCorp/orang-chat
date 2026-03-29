package com.orang.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoResponse {

    private String sessionId;
    private String connectedAt;     // ISO-8601 format
    private String lastActiveAt;    // ISO-8601 format
    private String userAgent;

    public static SessionInfoResponse from(String sessionId, Map<Object, Object> metadata) {
        String connectedAt = formatTimestamp(metadata.get("connectedAt"));
        String lastActiveAt = formatTimestamp(metadata.get("lastActiveAt"));
        String userAgent = (String) metadata.getOrDefault("userAgent", null);

        return SessionInfoResponse.builder()
                .sessionId(sessionId)
                .connectedAt(connectedAt)
                .lastActiveAt(lastActiveAt)
                .userAgent(userAgent)
                .build();
    }

    private static String formatTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return null;
        }

        try {
            long timestamp = Long.parseLong(timestampObj.toString());
            return Instant.ofEpochSecond(timestamp).toString();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}