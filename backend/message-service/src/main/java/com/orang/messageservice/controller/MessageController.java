package com.orang.messageservice.controller;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<List<MessageResponse>> getChatHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal String myUserId) {
        UUID userUUID = UUID.fromString(myUserId);
        return ResponseEntity.ok(messageService.getMessagesForConversation(conversationId, userUUID));
    }
}
