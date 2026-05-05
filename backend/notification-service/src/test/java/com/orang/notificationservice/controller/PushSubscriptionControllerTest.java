package com.orang.notificationservice.controller;

import com.orang.notificationservice.dto.PushSubscriptionResponse;
import com.orang.notificationservice.dto.SubscribeRequest;
import com.orang.notificationservice.dto.VapidKeyResponse;
import com.orang.notificationservice.service.PushSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionControllerTest {

    @Mock
    private PushSubscriptionService pushSubscriptionService;

    private PushSubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new PushSubscriptionController(pushSubscriptionService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "vapidPublicKey", "public-key-123");
    }

    @Test
    @DisplayName("getVapidPublicKey returns configured key")
    void getVapidPublicKeyReturnsConfiguredKey() {
        ResponseEntity<VapidKeyResponse> response = controller.getVapidPublicKey();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPublicKey()).isEqualTo("public-key-123");
    }

    @Test
    @DisplayName("subscribe delegates to service")
    void subscribeDelegatesToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        SubscribeRequest request = subscribeRequest();
        PushSubscriptionResponse responseBody = response();

        when(pushSubscriptionService.subscribe(userId, request, "Test Browser")).thenReturn(responseBody);

        ResponseEntity<PushSubscriptionResponse> response = controller.subscribe(request, userId.toString(), "Test Browser");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseBody);
        verify(pushSubscriptionService).subscribe(userId, request, "Test Browser");
    }

    @Test
    @DisplayName("unsubscribe delegates to service")
    void unsubscribeDelegatesToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");

        ResponseEntity<Void> response = controller.unsubscribe("https://push.example.com/subscriptions/endpoint-1234567890", userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(pushSubscriptionService).unsubscribe(userId, "https://push.example.com/subscriptions/endpoint-1234567890");
    }

    @Test
    @DisplayName("getSubscriptions delegates to service")
    void getSubscriptionsDelegatesToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        List<PushSubscriptionResponse> responses = List.of(response());

        when(pushSubscriptionService.getSubscriptions(userId)).thenReturn(responses);

        ResponseEntity<List<PushSubscriptionResponse>> response = controller.getSubscriptions(userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responses);
        verify(pushSubscriptionService).getSubscriptions(userId);
    }

    private SubscribeRequest subscribeRequest() {
        return SubscribeRequest.builder()
                .endpoint("https://push.example.com/subscriptions/endpoint-1234567890")
                .keys(SubscribeRequest.SubscriptionKeys.builder()
                        .p256dh("p256dh-key")
                        .auth("auth-key")
                        .build())
                .expirationTime(null)
                .build();
    }

    private PushSubscriptionResponse response() {
        return PushSubscriptionResponse.builder()
                .id(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
                .endpoint("https://push.example.com/subscriptions/endpoint-1234567890")
                .createdAt(LocalDateTime.of(2026, 5, 5, 9, 0))
                .expiresAt(LocalDateTime.of(2026, 5, 6, 9, 0))
                .userAgent("Test Browser")
                .build();
    }
}