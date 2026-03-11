package com.orang.messageservice.controller;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal String userId) {
        UUID userUUID = UUID.fromString(userId);
        return ResponseEntity.ok(conversationService.getConversations(userUUID));
    }

    @PostMapping("/direct/{targetUserId}")
    public ResponseEntity<ConversationResponse> getOrCreateDirectChat(
            @AuthenticationPrincipal String myUserId,
            @PathVariable UUID targetUserId) {
        UUID userUUID1 = UUID.fromString(myUserId);
        return ResponseEntity.ok(conversationService.getOrCreateDirectConversation(userUUID1, targetUserId));
    }
}
