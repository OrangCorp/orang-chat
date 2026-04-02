package com.orang.messageservice.controller;

import com.orang.messageservice.dto.ReadReceiptResponse;
import com.orang.messageservice.service.ReadReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/conversations/{conversationId}/read")
@RequiredArgsConstructor
public class ReadReceiptController {

    private final ReadReceiptService readReceiptService;

    @PostMapping("/{messageId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        log.debug("User {} marking message {} as read in conversation {}",
                userUUID, messageId, conversationId);

        readReceiptService.markAsRead(userUUID, conversationId, messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);
        long count = readReceiptService.getUnreadCount(userUUID, conversationId);
        return ResponseEntity.ok(count);
    }

    @GetMapping
    public ResponseEntity<ReadReceiptResponse> getReadReceipt(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {

        UUID userUUID = UUID.fromString(myUserId);

        UUID lastReadMessageId = readReceiptService.getLastReadMessageId(userUUID, conversationId)
                .orElse(null);
        long unreadCount = readReceiptService.getUnreadCount(userUUID, conversationId);

        ReadReceiptResponse response = ReadReceiptResponse.builder()
                .conversationId(conversationId)
                .lastReadMessageId(lastReadMessageId)
                .unreadCount(unreadCount)
                .build();

        return ResponseEntity.ok(response);
    }
}