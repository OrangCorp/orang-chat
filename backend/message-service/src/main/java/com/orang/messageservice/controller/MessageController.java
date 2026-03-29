package com.orang.messageservice.controller;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.dto.MessageSearchResponse;
import com.orang.messageservice.dto.MessagesAroundResponse;
import com.orang.messageservice.dto.PageRequestDto;
import com.orang.messageservice.service.MessageSearchService;
import com.orang.messageservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;
    private final MessageSearchService messageSearchService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getChatHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId,
            @Valid PageRequestDto pageRequest) {
        UUID userUUID = UUID.fromString(myUserId);
        Pageable pageable = pageRequest.toPageable();

        log.debug("Fetching messages for conversation {} with page {} and size {}",
                conversationId, pageRequest.getPage(), pageRequest.getSize());

        return ResponseEntity.ok(
                messageService.getMessagesForConversation(conversationId, userUUID, pageable)
        );    }

    @GetMapping("/{conversationId}/search")
    public ResponseEntity<Page<MessageSearchResponse>> searchMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId,
            @RequestParam("q") String query,
            @Valid PageRequestDto pageRequest) {
        UUID userUUID = UUID.fromString(myUserId);
        Pageable pageable = pageRequest.toPageable();

        return ResponseEntity.ok(
                messageSearchService.searchMessages(conversationId, userUUID, query, pageable)
        );
    }

    @GetMapping("/{conversationId}/around/{messageId}")
    public ResponseEntity<MessagesAroundResponse> getMessagesAround(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "50") int size) {
        UUID userUUID = UUID.fromString(myUserId);

        return ResponseEntity.ok(
                messageSearchService.getMessagesAround(conversationId, userUUID, messageId, size)
        );
    }
}
