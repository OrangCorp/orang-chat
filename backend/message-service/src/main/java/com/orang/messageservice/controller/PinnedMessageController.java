package com.orang.messageservice.controller;

import com.orang.messageservice.dto.PinnedMessagesResponse;
import com.orang.messageservice.service.PinnedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/conversations/{conversationId}/pins")
@RequiredArgsConstructor
public class PinnedMessageController {

    private final PinnedMessageService pinnedMessageService;

    @PostMapping("/{messageId}")
    public ResponseEntity<Void> pinMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        log.info("User {} pinning message {} in conversation {}", userUUID, messageId, conversationId);

        pinnedMessageService.pinMessage(conversationId, messageId, userUUID);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> unpinMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        log.info("User {} unpinning message {} in conversation {}", userUUID, messageId, conversationId);

        pinnedMessageService.unpinMessage(conversationId, messageId, userUUID);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PinnedMessagesResponse> getPinnedMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        List<UUID> pinnedMessageIds = pinnedMessageService.getPinnedMessageIds(conversationId, userUUID);

        PinnedMessagesResponse response = PinnedMessagesResponse.builder()
                .conversationId(conversationId)
                .pinnedMessageIds(pinnedMessageIds)
                .count(pinnedMessageIds.size())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<Boolean> isPinned(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {

        boolean pinned = pinnedMessageService.isPinned(conversationId, messageId);
        return ResponseEntity.ok(pinned);
    }
}