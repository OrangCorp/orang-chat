package com.orang.notificationservice.service;

import com.orang.notificationservice.dto.PushSubscriptionResponse;
import com.orang.notificationservice.dto.SubscribeRequest;
import com.orang.notificationservice.entity.PushSubscription;
import com.orang.notificationservice.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    private PushSubscriptionService pushSubscriptionService;

    private static final UUID USER_ID = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    private static final UUID OTHER_USER_ID = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    private static final String ENDPOINT = "https://push.example.com/subscriptions/endpoint-1234567890";

    @BeforeEach
    void setUp() {
        pushSubscriptionService = new PushSubscriptionService(pushSubscriptionRepository);
    }

    @Test
    @DisplayName("subscribe creates a new subscription when none exists")
    void subscribeCreatesNewSubscription() {
        SubscribeRequest request = request();
        PushSubscription savedSubscription = subscription(USER_ID, ENDPOINT, null, "Test Browser");

        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any(PushSubscription.class))).thenReturn(savedSubscription);

        PushSubscriptionResponse response = pushSubscriptionService.subscribe(USER_ID, request, "Test Browser");

        assertThat(response.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(response.getUserAgent()).isEqualTo("Test Browser");
        verify(pushSubscriptionRepository).save(any(PushSubscription.class));
    }

    @Test
    @DisplayName("subscribe updates an existing subscription")
    void subscribeUpdatesExistingSubscription() {
        SubscribeRequest request = request();
        PushSubscription existing = subscription(OTHER_USER_ID, ENDPOINT, null, null);
        PushSubscription saved = subscription(USER_ID, ENDPOINT, LocalDateTime.of(2026, 5, 5, 10, 0), "Updated Agent");

        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(existing));
        when(pushSubscriptionRepository.save(existing)).thenReturn(saved);

        PushSubscriptionResponse response = pushSubscriptionService.subscribe(USER_ID, request, "Updated Agent");

        assertThat(response.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(existing.getUserId()).isEqualTo(USER_ID);
        assertThat(existing.getUserAgent()).isEqualTo("Updated Agent");
        verify(pushSubscriptionRepository).save(existing);
    }

    @Test
    @DisplayName("unsubscribe deletes owned subscriptions and rejects foreign ones")
    void unsubscribeDeletesOwnedSubscriptionAndRejectsForeignOne() {
        PushSubscription owned = subscription(USER_ID, ENDPOINT, null, null);
        PushSubscription foreign = subscription(OTHER_USER_ID, ENDPOINT, null, null);

        when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty(), Optional.of(foreign), Optional.of(owned));

        pushSubscriptionService.unsubscribe(USER_ID, ENDPOINT);
        verify(pushSubscriptionRepository, never()).delete(any());

        assertThatThrownBy(() -> pushSubscriptionService.unsubscribe(USER_ID, ENDPOINT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");

        pushSubscriptionService.unsubscribe(USER_ID, ENDPOINT);
        verify(pushSubscriptionRepository).delete(owned);
    }

    @Test
    @DisplayName("getSubscriptions and getActiveSubscriptions map and filter results")
    void getSubscriptionsAndGetActiveSubscriptionsMapAndFilterResults() {
        PushSubscription active = subscription(USER_ID, ENDPOINT, LocalDateTime.now().plusDays(1), null);
        PushSubscription expired = subscription(USER_ID, ENDPOINT + "/expired", LocalDateTime.now().minusDays(1), null);
        PushSubscription noExpiry = subscription(USER_ID, ENDPOINT + "/no-expiry", null, null);

        when(pushSubscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(active, expired, noExpiry));

        List<PushSubscriptionResponse> responses = pushSubscriptionService.getSubscriptions(USER_ID);
        assertThat(responses).hasSize(3);
        assertThat(responses.getFirst().getEndpoint()).isEqualTo(ENDPOINT);

        List<PushSubscription> activeSubscriptions = pushSubscriptionService.getActiveSubscriptions(USER_ID);
        assertThat(activeSubscriptions).containsExactly(active, noExpiry);
    }

    private SubscribeRequest request() {
        return SubscribeRequest.builder()
                .endpoint(ENDPOINT)
                .keys(SubscribeRequest.SubscriptionKeys.builder()
                        .p256dh("p256dh-key")
                        .auth("auth-key")
                        .build())
                .expirationTime(null)
                .build();
    }

    private PushSubscription subscription(UUID userId, String endpoint, LocalDateTime expiresAt, String userAgent) {
        PushSubscription subscription = PushSubscription.builder()
                .id(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
                .userId(userId)
                .endpoint(endpoint)
                .p256dhKey("p256dh-key")
                .authKey("auth-key")
                .expiresAt(expiresAt)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.of(2026, 5, 5, 9, 0))
                .lastUsedAt(LocalDateTime.of(2026, 5, 5, 9, 30))
                .build();
        return subscription;
    }
}