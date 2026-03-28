package com.orang.userservice.controller;

import com.orang.shared.presence.UserStatus;
import com.orang.userservice.dto.BatchStatusRequest;
import com.orang.userservice.dto.LastSeenResponse;
import com.orang.userservice.dto.SessionInfoResponse;
import com.orang.userservice.dto.UserStatusResponse;
import com.orang.userservice.service.PresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}/status")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable String userId) {
        UserStatus status = presenceService.getUserStatus(userId);
        UserStatusResponse response = UserStatusResponse.from(userId, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/status/batch")
    public ResponseEntity<List<UserStatusResponse>> getBatchStatus(
            @Valid @RequestBody BatchStatusRequest request) {

        Map<String, UserStatus> statusMap = presenceService.getBatchUserStatus(request.getUserIds());

        List<UserStatusResponse> responses = new ArrayList<>();
        for (Map.Entry<String, UserStatus> entry : statusMap.entrySet()) {
            responses.add(UserStatusResponse.from(entry.getKey(), entry.getValue()));
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{userId}/last-seen")
    public ResponseEntity<LastSeenResponse> getLastSeen(@PathVariable String userId) {
        Long lastActivityTime = presenceService.getLastActivityTime(userId);
        boolean isOnline = presenceService.isUserOnline(userId);

        LastSeenResponse response = LastSeenResponse.from(userId, lastActivityTime, isOnline);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/sessions")
    public ResponseEntity<List<SessionInfoResponse>> getUserSessions(@PathVariable String userId) {
        Set<String> sessionIds = presenceService.getActiveSessions(userId);

        List<SessionInfoResponse> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            Map<Object, Object> metadata = presenceService.getSessionMetadata(sessionId);
            sessions.add(SessionInfoResponse.from(sessionId, metadata));
        }

        return ResponseEntity.ok(sessions);
    }
}
