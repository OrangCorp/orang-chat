package com.orang.chatservice.controller;

import com.orang.chatservice.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceController {

    private final PresenceService presenceService;

    @MessageMapping("/presence.heartbeat")
    public void handleHeartbeat(Principal userPrincipal,
                                StompHeaderAccessor accessor) {
        if (userPrincipal != null) {
            String userId = userPrincipal.getName();
            String sessionId = accessor.getSessionId();

            if (sessionId == null) {
                return;
            }

            presenceService.refreshSession(userId, sessionId);

            log.debug("Heartbeat received for user: {}, session: {}", userId, sessionId);
        }
    }
}
