package com.orang.chatservice.controller;

import com.orang.chatservice.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage message) {
        log.info("Received message from {} to {}", message.getSenderId(), message.getRecipientId());

        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
                message.getRecipientId().toString(),
                "/queue/messages",
                message
        );
    }
}
