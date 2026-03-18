package com.orang.messageservice.controller;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.dto.PageRequestDto;
import com.orang.messageservice.service.MessageService;
import com.orang.shared.constants.PaginationConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
}
