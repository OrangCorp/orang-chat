package com.chatapp.notificationservice.service;

import com.chatapp.notificationservice.dto.NotificationPreferencesResponse;
import com.chatapp.notificationservice.entity.NotificationPreferences;
import com.chatapp.notificationservice.entity.NotificationPreferencesId;
import com.chatapp.notificationservice.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;

    @Transactional
    public void mute(UUID userId, UUID conversationId, LocalDateTime until) {
        NotificationPreferencesId id = new NotificationPreferencesId(userId, conversationId);

        NotificationPreferences prefs = preferencesRepository.findById(id)
                .orElse(NotificationPreferences.builder()
                        .id(id)
                        .build());

        prefs.setMuted(true);
        prefs.setMutedUntil(until);

        preferencesRepository.save(prefs);

        if (until == null) {
            log.info("User {} permanently muted conversation {}", userId, conversationId);
        } else {
            log.info("User {} muted conversation {} until {}", userId, conversationId, until);
        }
    }

    public void unmute(UUID userId, UUID conversationId) {
        NotificationPreferencesId id = new NotificationPreferencesId(userId, conversationId);

        Optional<NotificationPreferences> prefs = preferencesRepository.findById(id);

        if (prefs.isEmpty()) {
            log.debug("User {} attempted to unmute already-unmuted conversation {}",
                    userId, conversationId);
            return;
        }

        prefs.get().setMuted(false);
        prefs.get().setMutedUntil(null);
        preferencesRepository.save(prefs.get());

        log.info("User {} unmuted conversation {}", userId, conversationId);
    }

    @Transactional
    public boolean isMuted(UUID userId, UUID conversationId) {
        NotificationPreferencesId id = new NotificationPreferencesId(userId, conversationId);
        Optional<NotificationPreferences> prefs = preferencesRepository.findById(id);

        if (prefs.isEmpty()) {
            return false;
        }

        if (!prefs.get().isMuted()) {
            return false;
        }

        LocalDateTime mutedUntil = prefs.get().getMutedUntil();
        if (mutedUntil != null && mutedUntil.isBefore(LocalDateTime.now())) {
            // Temporary mute expired - auto-unmute
            log.info("Auto-unmuting conversation {} for user {} (expired at {})",
                    conversationId, userId, mutedUntil);

            prefs.get().setMuted(false);
            prefs.get().setMutedUntil(null);
            preferencesRepository.save(prefs.get());

            return false;
        }

        return true;
    }

    public NotificationPreferencesResponse getPreferences(UUID userId, UUID conversationId) {
        NotificationPreferencesId id = new NotificationPreferencesId(userId, conversationId);

        return preferencesRepository.findById(id)
                .map(prefs -> NotificationPreferencesResponse.builder()
                        .conversationId(conversationId)
                        .muted(prefs.isMuted())
                        .mutedUntil(prefs.getMutedUntil())
                        .build())
                .orElse(NotificationPreferencesResponse.builder()
                        .conversationId(conversationId)
                        .muted(false)
                        .mutedUntil(null)
                        .build());
    }

    public List<UUID> getMutedConversations(UUID userId) {
        return preferencesRepository.findByIdUserIdAndMutedTrue(userId).stream()
                .map(prefs -> prefs.getId().getConversationId())
                .toList();
    }
}
