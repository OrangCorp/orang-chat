package com.orang.userservice.controller;

import com.orang.shared.presence.UserStatus;
import com.orang.userservice.dto.BatchStatusRequest;
import com.orang.userservice.dto.LastSeenResponse;
import com.orang.userservice.dto.SessionInfoResponse;
import com.orang.userservice.dto.UserStatusResponse;
import com.orang.userservice.service.PresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private PresenceController presenceController;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_SESSION_ID = "session-abc";

    @Test
    @DisplayName("getUserStatus returns correct status")
    void getUserStatus_ReturnsStatus() {
        // Arrange
        when(presenceService.getUserStatus(TEST_USER_ID)).thenReturn(UserStatus.ONLINE);

        // Act
        ResponseEntity<UserStatusResponse> response = presenceController.getUserStatus(TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getBody().getStatus()).isEqualTo(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("getLastSeen returns last activity info")
    void getLastSeen_ReturnsLastActivity() {
        // Arrange
        long timestamp = 1774455000L;
        when(presenceService.getLastActivityTime(TEST_USER_ID)).thenReturn(timestamp);
        when(presenceService.isUserOnline(TEST_USER_ID)).thenReturn(true);

        // Act
        ResponseEntity<LastSeenResponse> response = presenceController.getLastSeen(TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getBody().getLastSeenAt()).isNotNull();
        assertThat(response.getBody().isOnline()).isTrue();
    }

    @Test
    @DisplayName("getLastSeen returns null timestamp when never active")
    void getLastSeen_ReturnsNullWhenNeverActive() {
        // Arrange
        when(presenceService.getLastActivityTime(TEST_USER_ID)).thenReturn(null);
        when(presenceService.isUserOnline(TEST_USER_ID)).thenReturn(false);

        // Act
        ResponseEntity<LastSeenResponse> response = presenceController.getLastSeen(TEST_USER_ID);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLastSeenAt()).isNull();
        assertThat(response.getBody().isOnline()).isFalse();
    }

    @Test
    @DisplayName("getUserSessions returns list of sessions")
    void getUserSessions_ReturnsSessions() {
        // Arrange
        Set<String> sessions = Set.of(TEST_SESSION_ID);
        Map<Object, Object> metadata = new HashMap<>();
        metadata.put("userId", TEST_USER_ID);
        metadata.put("connectedAt", "1774455000");
        metadata.put("lastActiveAt", "1774455000");
        metadata.put("userAgent", "Chrome");

        when(presenceService.getActiveSessions(TEST_USER_ID)).thenReturn(sessions);
        when(presenceService.getSessionMetadata(TEST_SESSION_ID)).thenReturn(metadata);

        // Act
        ResponseEntity<List<SessionInfoResponse>> response = presenceController.getUserSessions(TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getSessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(response.getBody().getFirst().getUserAgent()).isEqualTo("Chrome");
    }

    @Test
    @DisplayName("getUserSessions returns empty list when no sessions")
    void getUserSessions_ReturnsEmptyList() {
        // Arrange
        when(presenceService.getActiveSessions(TEST_USER_ID)).thenReturn(Set.of());

        // Act
        ResponseEntity<List<SessionInfoResponse>> response = presenceController.getUserSessions(TEST_USER_ID);

        // Assert
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("getBatchStatus returns status for multiple users")
    void getBatchStatus_ReturnsMultipleStatuses() {
        // Arrange
        BatchStatusRequest request = new BatchStatusRequest();
        request.setUserIds(List.of("user-1", "user-2"));

        when(presenceService.getBatchUserStatus(List.of("user-1", "user-2")))
                .thenReturn(Map.of(
                        "user-1", UserStatus.ONLINE,
                        "user-2", UserStatus.OFFLINE
                ));

        // Act
        ResponseEntity<List<UserStatusResponse>> response = presenceController.getBatchStatus(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("terminateSession returns 204 when session terminated successfully")
    void terminateSession_ValidSession_Returns204() {
        // Arrange
        Map<Object, Object> metadata = new HashMap<>();
        metadata.put("userId", TEST_USER_ID);

        when(presenceService.getSessionMetadata(TEST_SESSION_ID)).thenReturn(metadata);
        when(presenceService.terminateSession(TEST_SESSION_ID)).thenReturn(true);

        // Act — user terminates their own session
        ResponseEntity<Void> response = presenceController.terminateSession(
                TEST_USER_ID, TEST_SESSION_ID, TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(presenceService).terminateSession(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("terminateSession returns 404 when session doesn't exist")
    void terminateSession_NonexistentSession_Returns404() {
        // Arrange
        when(presenceService.getSessionMetadata(TEST_SESSION_ID)).thenReturn(Map.of());

        // Act
        ResponseEntity<Void> response = presenceController.terminateSession(
                TEST_USER_ID, TEST_SESSION_ID, TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("terminateSession returns 403 when user doesn't own the session")
    void terminateSession_DifferentUser_Returns403() {
        // Act — "different-user" tries to terminate TEST_USER_ID's session
        ResponseEntity<Void> response = presenceController.terminateSession(
                TEST_USER_ID, TEST_SESSION_ID, "different-user");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("terminateSession returns 403 when session belongs to different user")
    void terminateSession_SessionBelongsToDifferentUser_Returns403() {
        // Arrange — session metadata shows different owner
        Map<Object, Object> metadata = new HashMap<>();
        metadata.put("userId", "other-user-id");

        when(presenceService.getSessionMetadata(TEST_SESSION_ID)).thenReturn(metadata);

        // Act
        ResponseEntity<Void> response = presenceController.terminateSession(
                TEST_USER_ID, TEST_SESSION_ID, TEST_USER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}