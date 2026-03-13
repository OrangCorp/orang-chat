package com.orang.messageservice.controller;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getChatHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userUUID = UUID.fromString(myUserId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return ResponseEntity.ok(
                messageService.getMessagesForConversation(conversationId, userUUID, pageable)
        );
    }
}
