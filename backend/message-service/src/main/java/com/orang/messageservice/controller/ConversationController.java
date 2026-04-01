package com.orang.messageservice.controller;

import com.orang.messageservice.dto.AddParticipantsRequest;
import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.dto.CreateGroupRequest;
import com.orang.messageservice.dto.RenameConversationRequest;
import com.orang.messageservice.entity.ConversationParticipant;
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

    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<ConversationResponse> addParticipants(
            @AuthenticationPrincipal String myUserId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody AddParticipantsRequest request) {
        UUID userUUID = UUID.fromString(myUserId);
        ConversationResponse conversation = conversationService.addParticipants(
                conversationId,
                request.getUserIds(),
                userUUID);
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/{conversationId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID conversationId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        conversationService.removeParticipant(conversationId, userId, myUserUUID);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        conversationService.leaveConversation(conversationId, myUserUUID);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> renameConversation(
            @PathVariable UUID conversationId,
            @RequestBody @Valid RenameConversationRequest request,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        ConversationResponse conversation = conversationService.renameConversation(
                conversationId,
                request.getName(),
                myUserUUID);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/{conversationId}/participants/{userId}/promote")
    public ResponseEntity<ConversationResponse> promoteParticipant(
            @PathVariable UUID conversationId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        ConversationResponse conversation = conversationService.promoteParticipant(
                conversationId,
                userId,
                myUserUUID);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/{conversationId}/participants/{userId}/demote")
    public ResponseEntity<ConversationResponse> demoteParticipant(
            @PathVariable UUID conversationId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        ConversationResponse conversation = conversationService.demoteParticipant(
                conversationId,
                userId,
                myUserUUID);
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {
        UUID myUserUUID = UUID.fromString(myUserId);
        conversationService.deleteConversation(conversationId, myUserUUID);
        return ResponseEntity.noContent().build();
    }
}
