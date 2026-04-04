package com.orang.messageservice.controller;

import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/messages/{messageId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/{reactionType}")
    public ResponseEntity<Map<ReactionType, Long>> toggleReaction(
            @PathVariable UUID messageId,
            @PathVariable ReactionType reactionType,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        log.info("User {} toggling {} reaction on message {}", userUUID, reactionType, messageId);

        Map<ReactionType, Long> counts = reactionService.toggleReaction(messageId, userUUID, reactionType);
        return ResponseEntity.ok(counts);
    }

    @GetMapping
    public ResponseEntity<Map<ReactionType, Long>> getReactions(@PathVariable UUID messageId) {
        Map<ReactionType, Long> counts = reactionService.getReactionCounts(messageId);
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/mine")
    public ResponseEntity<Optional<ReactionType>> getMyReaction(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);
        Optional<ReactionType> reaction = reactionService.getReactionType(messageId, userUUID);
        return ResponseEntity.ok(reaction);
    }
}