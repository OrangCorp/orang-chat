package com.chatapp.notificationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {

    @NotBlank(message = "Endpoint is required")
    private String endpoint;

    @NotNull(message = "Keys are required")
    @Valid
    private SubscriptionKeys keys;

    private Long expirationTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionKeys {

        @NotBlank(message = "p256dh key is required")
        private String p256dh;

        @NotBlank(message = "auth key is required")
        private String auth;
    }
}
