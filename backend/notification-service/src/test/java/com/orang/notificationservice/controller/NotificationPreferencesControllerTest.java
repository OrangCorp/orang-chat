package com.orang.notificationservice.controller;

import com.orang.notificationservice.dto.MuteRequest;
import com.orang.notificationservice.dto.NotificationPreferencesResponse;
import com.orang.notificationservice.service.NotificationPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesControllerTest {

    @Mock
    private NotificationPreferencesService preferencesService;

    private NotificationPreferencesController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationPreferencesController(preferencesService);
    }

    @Test
    @DisplayName("mute delegates permanent mute to service")
    void muteDelegatesPermanentMuteToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID conversationId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        ResponseEntity<Void> response = controller.mute(conversationId, null, userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(preferencesService).mute(userId, conversationId, null);
    }

    @Test
    @DisplayName("mute delegates timed mute to service")
    void muteDelegatesTimedMuteToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID conversationId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        LocalDateTime until = LocalDateTime.of(2026, 5, 6, 12, 0);

        ResponseEntity<Void> response = controller.mute(conversationId, MuteRequest.builder().until(until).build(), userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(preferencesService).mute(userId, conversationId, until);
    }

    @Test
    @DisplayName("unmute delegates to service")
    void unmuteDelegatesToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID conversationId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        ResponseEntity<Void> response = controller.unmute(conversationId, userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(preferencesService).unmute(userId, conversationId);
    }

    @Test
    @DisplayName("getPreferences delegates to service")
    void getPreferencesDelegatesToService() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID conversationId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        NotificationPreferencesResponse responseBody = NotificationPreferencesResponse.builder()
                .conversationId(conversationId)
                .muted(true)
                .mutedUntil(LocalDateTime.of(2026, 5, 6, 12, 0))
                .build();

        when(preferencesService.getPreferences(userId, conversationId)).thenReturn(responseBody);

        ResponseEntity<NotificationPreferencesResponse> response = controller.getPreferences(conversationId, userId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);
        verify(preferencesService).getPreferences(userId, conversationId);
    }
}