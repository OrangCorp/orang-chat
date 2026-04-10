package com.orang.notificationservice.service;

import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.entity.PushSubscription;
import com.orang.notificationservice.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for sending web push notifications.
 *
 * Handles:
 * - Sending notifications to individual users
 * - Sending to conversation participants (with mute filtering)
 * - Handling push service responses (success, expired, error)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final NotificationPreferencesService preferencesService;
    private final ObjectMapper objectMapper;

    @Async
    public void sendToUser(UUID userId, NotificationPayload payload) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);

        if (subscriptions.isEmpty()) {
            log.debug("No push subscriptions for user {}", userId);
            return;
        }

        String jsonPayload = toJson(payload);

        log.info("Sending notification to user {} ({} devices)", userId, subscriptions.size());

        for (PushSubscription subscription : subscriptions) {
            sendToSubscription(subscription, jsonPayload);
        }
    }

    @Async
    public void sendToConversation(
            UUID conversationId,
            Set<UUID> participantIds,
            UUID senderId,
            NotificationPayload payload) {

        log.debug("Sending notification for conversation {} to {} participants (excluding sender {})",
                conversationId, participantIds.size(), senderId);

        for (UUID participantId : participantIds) {
            if (participantId.equals(senderId)) {
                continue;
            }

            if (preferencesService.isMuted(participantId, conversationId)) {
                log.debug("Skipping notification to user {} - conversation {} is muted",
                        participantId, conversationId);
                continue;
            }

            sendToUser(participantId, payload);
        }
    }

    private void sendToSubscription(PushSubscription subscription, String payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dhKey(),
                    subscription.getAuthKey(),
                    payload.getBytes()
            );

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201) {
                // Success - update last used timestamp
                subscription.setLastUsedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);

                log.debug("Successfully sent notification to endpoint: {}...",
                        truncateEndpoint(subscription.getEndpoint()));

            } else if (statusCode == 410) {
                // Gone - subscription no longer valid
                log.warn("Subscription expired (410), removing: {}...",
                        truncateEndpoint(subscription.getEndpoint()));

                subscriptionRepository.delete(subscription);

            } else if (statusCode == 404) {
                // Not found - subscriptions invalid
                log.warn("Subscription not found (404), removing: {}...",
                        truncateEndpoint(subscription.getEndpoint()));

                subscriptionRepository.delete(subscription);

            } else {
                // Other error
                log.error("Failed to send notification. Status: {}, Endpoint: {}...",
                        statusCode,
                        truncateEndpoint(subscription.getEndpoint()));
            }

        } catch (Exception e) {
            log.error("Exception sending notification to {}...: {}",
                    truncateEndpoint(subscription.getEndpoint()),
                    e.getMessage());
        }
    }

    private String toJson(NotificationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification payload", e);
            throw new RuntimeException("Failed to serialize notification payload", e);
        }
    }

    private String truncateEndpoint(String endpoint) {
        return endpoint.substring(0, Math.min(50, endpoint.length()));
    }
}