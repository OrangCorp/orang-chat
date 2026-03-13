package com.orang.messageservice.controller;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.dto.CreateGroupRequest;
import com.orang.messageservice.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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

    @PostMapping("/group")
    public ResponseEntity<ConversationResponse> createGroupChat(
            @AuthenticationPrincipal String myUserId,
            @Valid @RequestBody CreateGroupRequest request) {
        UUID userUUID1 = UUID.fromString(myUserId);
        ConversationResponse conversation = conversationService.createGroupConversation(
                request.getName(),
                request.getParticipantIds(),
                userUUID1);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }
}
