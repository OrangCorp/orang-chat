package com.orang.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPayload {

    private String title;
    private String body;
    private String icon;
    private String badge;
    private String tag;
    private Boolean requireInteraction;
    private NotificationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationData {

        private String type;
        private UUID conversationId;
        private UUID messageId;
        private String url;
    }
}
