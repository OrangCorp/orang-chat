package com.orang.notificationservice.controller;

import com.orang.notificationservice.dto.PushSubscriptionResponse;
import com.orang.notificationservice.dto.SubscribeRequest;
import com.orang.notificationservice.dto.VapidKeyResponse;
import com.orang.notificationservice.service.PushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @Value("${web-push.vapid.public-key}")
    private String vapidPublicKey;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidKeyResponse> getVapidPublicKey() {
        log.debug("VAPID public key requested");

        return ResponseEntity.ok(
                VapidKeyResponse.builder()
                        .publicKey(vapidPublicKey)
                        .build()
        );
    }

    @PostMapping("/subscribe")
    public ResponseEntity<PushSubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        UUID userUUID = UUID.fromString(userId);

        log.info("User {} subscribing to push notifications from: {}",
                userUUID, userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "unknown");

        PushSubscriptionResponse response = pushSubscriptionService.subscribe(
                userUUID,
                request,
                userAgent
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestParam String endpoint,
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);

        log.info("User {} unsubscribing endpoint: {}...",
                userUUID, endpoint.substring(0, Math.min(50, endpoint.length())));

        pushSubscriptionService.unsubscribe(userUUID, endpoint);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<PushSubscriptionResponse>> getSubscriptions(
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);

        log.debug("User {} listing subscriptions", userUUID);

        List<PushSubscriptionResponse> subscriptions =
                pushSubscriptionService.getSubscriptions(userUUID);

        return ResponseEntity.ok(subscriptions);
    }
}