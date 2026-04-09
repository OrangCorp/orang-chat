package com.chatapp.notificationservice.controller;

import com.chatapp.notificationservice.dto.MuteRequest;
import com.chatapp.notificationservice.dto.NotificationPreferencesResponse;
import com.chatapp.notificationservice.service.NotificationPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations/{conversationId}/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    @PostMapping("/mute")
    public ResponseEntity<Void> mute(
            @PathVariable UUID conversationId,
            @RequestBody(required = false) MuteRequest request,
            @AuthenticationPrincipal String userId) {
        UUID userUUID = UUID.fromString(userId);

        if (request != null && request.getUntil() != null) {
            log.info("User {} muting conversation {} until {}", userUUID, conversationId, request.getUntil());
        } else {
            log.info("User {} permanently muting conversation {}", userUUID, conversationId);
        }

        preferencesService.mute(
                userUUID,
                conversationId,
                request != null ? request.getUntil() : null
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/unmute")
    public ResponseEntity<Void> unmute(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);

        log.info("User {} unmuting conversation {}", userUUID, conversationId);

        preferencesService.unmute(userUUID, conversationId);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);

        log.debug("User {} getting notification preferences for conversation {}", userUUID, conversationId);

        NotificationPreferencesResponse response = preferencesService.getPreferences(userUUID, conversationId);

        return ResponseEntity.ok(response);
    }
}
