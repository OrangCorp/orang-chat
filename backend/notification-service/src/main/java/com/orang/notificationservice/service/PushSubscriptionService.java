package com.orang.notificationservice.service;

import com.orang.notificationservice.dto.PushSubscriptionResponse;
import com.orang.notificationservice.dto.SubscribeRequest;
import com.orang.notificationservice.entity.PushSubscription;
import com.orang.notificationservice.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    public PushSubscriptionResponse subscribe(UUID userId, SubscribeRequest request, String userAgent) {
        String endpoint = request.getEndpoint();

        Optional<PushSubscription> existingSubscription = pushSubscriptionRepository.findByEndpoint(endpoint);

        PushSubscription subscription;

        if (existingSubscription.isPresent()) {
            subscription = existingSubscription.get();
            subscription.setUserId(userId);
            subscription.setP256dhKey(request.getKeys().getP256dh());
            subscription.setAuthKey(request.getKeys().getAuth());
            subscription.setExpiresAt(convertExpirationTime(request.getExpirationTime()));
            subscription.setUserAgent(userAgent);
            subscription.setLastUsedAt(LocalDateTime.now());

            log.info("Updated push subscription for user {}: {}...",
                    userId, endpoint.substring(0, Math.min(50, endpoint.length())));
        } else {
            subscription = PushSubscription.builder()
                    .userId(userId)
                    .endpoint(endpoint)
                    .p256dhKey(request.getKeys().getP256dh())
                    .authKey(request.getKeys().getAuth())
                    .expiresAt(convertExpirationTime(request.getExpirationTime()))
                    .userAgent(userAgent)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
        }

        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        Optional<PushSubscription> subscription = pushSubscriptionRepository.findByEndpoint(endpoint);

        if (subscription.isEmpty()) {
            log.warn("Unsubscribe requested for non-existent endpoint: {}...",
                    endpoint.substring(0, Math.min(50, endpoint.length())));
            return;
        }

        if (!subscription.get().getUserId().equals(userId)) {
            log.warn("User {} attempted to unsubscribe endpoint belonging to user {}",
                    userId, subscription.get().getUserId());
            throw new IllegalArgumentException("Subscription does not belong to this user");
        }

        pushSubscriptionRepository.delete(subscription.get());
        log.info("Deleted push subscription for user {}: {}...",
                userId, endpoint.substring(0, Math.min(50, endpoint.length())));

    }

    public List<PushSubscriptionResponse> getSubscriptions(UUID userId) {
        return pushSubscriptionRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PushSubscription> getActiveSubscriptions(UUID userId) {
        return pushSubscriptionRepository.findByUserId(userId).stream()
                .filter(sub -> !isExpired(sub))
                .toList();
    }

    private boolean isExpired(PushSubscription subscription) {
        if (subscription.getExpiresAt() == null) {
            return false;
        }
        return subscription.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private PushSubscriptionResponse toResponse(PushSubscription subscription) {
        return PushSubscriptionResponse.builder()
                .id(subscription.getId())
                .endpoint(subscription.getEndpoint())
                .createdAt(subscription.getCreatedAt())
                .expiresAt(subscription.getExpiresAt())
                .userAgent(subscription.getUserAgent())
                .build();
    }

    private LocalDateTime convertExpirationTime(Long expirationTime) {
        if (expirationTime == null) {
            return null;
        }
        return Instant.ofEpochMilli(expirationTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
