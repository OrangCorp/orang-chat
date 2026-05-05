package com.orang.notificationservice.service;

import com.orang.notificationservice.dto.NotificationPreferencesResponse;
import com.orang.notificationservice.entity.NotificationPreferences;
import com.orang.notificationservice.entity.NotificationPreferencesId;
import com.orang.notificationservice.repository.NotificationPreferencesRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    private NotificationPreferencesService preferencesService;

    private static final UUID USER_ID = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    private static final UUID CONVERSATION_ID = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

    @BeforeEach
    void setUp() {
        preferencesService = new NotificationPreferencesService(preferencesRepository);
    }

    @Test
    @DisplayName("mute creates or updates preferences")
    void muteCreatesOrUpdatesPreferences() {
        NotificationPreferences existing = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(false)
                .build();

        when(preferencesRepository.findById(new NotificationPreferencesId(USER_ID, CONVERSATION_ID)))
                .thenReturn(Optional.empty(), Optional.of(existing));

        preferencesService.mute(USER_ID, CONVERSATION_ID, null);
        preferencesService.mute(USER_ID, CONVERSATION_ID, LocalDateTime.of(2026, 5, 6, 12, 0));

        verify(preferencesRepository).save(existing);
    }

    @Test
    @DisplayName("unmute ignores missing preferences and resets stored ones")
    void unmuteIgnoresMissingAndResetsStoredOnes() {
        NotificationPreferences existing = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(true)
                .mutedUntil(LocalDateTime.of(2026, 5, 6, 12, 0))
                .build();

        when(preferencesRepository.findById(new NotificationPreferencesId(USER_ID, CONVERSATION_ID)))
                .thenReturn(Optional.empty(), Optional.of(existing));

        preferencesService.unmute(USER_ID, CONVERSATION_ID);
        verify(preferencesRepository, never()).save(existing);

        preferencesService.unmute(USER_ID, CONVERSATION_ID);
        assertThat(existing.isMuted()).isFalse();
        assertThat(existing.getMutedUntil()).isNull();
        verify(preferencesRepository).save(existing);
    }

    @Test
    @DisplayName("isMuted handles missing, expired, and active mutes")
    void isMutedHandlesMissingExpiredAndActiveMutes() {
        NotificationPreferences expired = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(true)
                .mutedUntil(LocalDateTime.now().minusMinutes(5))
                .build();
        NotificationPreferences active = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(true)
                .mutedUntil(LocalDateTime.now().plusMinutes(5))
                .build();
        NotificationPreferences notMuted = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(false)
                .build();

        when(preferencesRepository.findById(new NotificationPreferencesId(USER_ID, CONVERSATION_ID)))
                .thenReturn(Optional.empty(), Optional.of(expired), Optional.of(active), Optional.of(notMuted));

        assertThat(preferencesService.isMuted(USER_ID, CONVERSATION_ID)).isFalse();
        assertThat(preferencesService.isMuted(USER_ID, CONVERSATION_ID)).isFalse();
        assertThat(preferencesService.isMuted(USER_ID, CONVERSATION_ID)).isTrue();
        assertThat(preferencesService.isMuted(USER_ID, CONVERSATION_ID)).isFalse();

        verify(preferencesRepository).save(expired);
    }

    @Test
    @DisplayName("getPreferences returns stored and default values")
    void getPreferencesReturnsStoredAndDefaultValues() {
        NotificationPreferences stored = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, CONVERSATION_ID))
                .muted(true)
                .mutedUntil(LocalDateTime.of(2026, 5, 6, 12, 0))
                .build();

        when(preferencesRepository.findById(new NotificationPreferencesId(USER_ID, CONVERSATION_ID)))
                .thenReturn(Optional.of(stored), Optional.empty());

        NotificationPreferencesResponse existing = preferencesService.getPreferences(USER_ID, CONVERSATION_ID);
        NotificationPreferencesResponse defaultResponse = preferencesService.getPreferences(USER_ID, CONVERSATION_ID);

        assertThat(existing.isMuted()).isTrue();
        assertThat(existing.getMutedUntil()).isEqualTo(stored.getMutedUntil());
        assertThat(defaultResponse.isMuted()).isFalse();
        assertThat(defaultResponse.getMutedUntil()).isNull();
    }

    @Test
    @DisplayName("getMutedConversations maps muted preferences to conversation IDs")
    void getMutedConversationsMapsMutedPreferencesToConversationIds() {
        UUID firstConversation = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");
        UUID secondConversation = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");

        NotificationPreferences first = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, firstConversation))
                .muted(true)
                .build();
        NotificationPreferences second = NotificationPreferences.builder()
                .id(new NotificationPreferencesId(USER_ID, secondConversation))
                .muted(true)
                .build();

        when(preferencesRepository.findByIdUserIdAndMutedTrue(USER_ID)).thenReturn(List.of(first, second));

        assertThat(preferencesService.getMutedConversations(USER_ID)).containsExactly(firstConversation, secondConversation);
    }
}