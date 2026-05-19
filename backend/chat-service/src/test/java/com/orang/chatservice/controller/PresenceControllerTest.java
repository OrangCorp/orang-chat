package com.orang.chatservice.controller;

import com.orang.chatservice.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private PresenceController presenceController;

    @Mock
    private Principal userPrincipal;

    @Mock
    private StompHeaderAccessor accessor;

    private String userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        sessionId = "session-" + UUID.randomUUID();
    }

    @Test
    void handleHeartbeatRefreshesSession() {
        when(userPrincipal.getName()).thenReturn(userId);
        when(accessor.getSessionId()).thenReturn(sessionId);

        presenceController.handleHeartbeat(userPrincipal, accessor);

        verify(presenceService).refreshSession(userId, sessionId);
    }

    @Test
    void handleHeartbeatDropsWhenUserPrincipalNull() {
        presenceController.handleHeartbeat(null, accessor);

        verifyNoInteractions(presenceService);
    }

    @Test
    void handleHeartbeatDropsWhenSessionIdNull() {
        when(userPrincipal.getName()).thenReturn(userId);
        when(accessor.getSessionId()).thenReturn(null);

        presenceController.handleHeartbeat(userPrincipal, accessor);

        verifyNoInteractions(presenceService);
    }
}
