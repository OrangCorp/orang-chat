package com.orang.chatservice.listener;

import com.orang.chatservice.service.PresenceService;
import com.orang.shared.constants.PresenceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = accessor.getUser();

        if (userPrincipal != null) {
            String userId = userPrincipal.getName();
            String sessionId = accessor.getSessionId();

            Map<String, String> metadata = new HashMap<>();

            String userAgent = accessor.getFirstNativeHeader("User-Agent");
            if (userAgent != null) {
                metadata.put(PresenceConstants.META_USER_AGENT, userAgent);
            }

            presenceService.addSession(userId, sessionId, metadata);
            log.info("User connected: {}", userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = accessor.getUser();

        if (userPrincipal != null) {
            String userId = userPrincipal.getName();
            String sessionId = accessor.getSessionId();

            presenceService.removeSession(userId, sessionId);
            log.info("User disconnected: {}", userId);
        }
    }
}
