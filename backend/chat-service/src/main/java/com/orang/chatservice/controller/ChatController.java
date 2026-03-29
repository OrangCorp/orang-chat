package com.orang.chatservice.controller;

import com.orang.chatservice.dto.ChatMessage;
import com.orang.chatservice.dto.MessageType;
import com.orang.chatservice.service.PresenceService;
import com.orang.shared.event.MessageReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final PresenceService presenceService;

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage message) {
        log.info("Received {} message from {} to {}",
                message.getType() ,
                message.getSenderId(),
                message.getRecipientId());

        if (message.getSenderId().equals(message.getRecipientId())) {
            throw new IllegalArgumentException("You cannot send a message to yourself");
        }

        presenceService.updateLastActivity(message.getSenderId().toString());

        message.setTimestamp(LocalDateTime.now());

        if (MessageType.GROUP.equals(message.getType())) {
            messagingTemplate.convertAndSend(
                    "/topic/group/" + message.getRecipientId(),
                    message
            );
        } else {
            messagingTemplate.convertAndSendToUser(
                    message.getRecipientId().toString(),
                    "/queue/messages",
                    message
            );
        }

        if (!MessageType.TYPING.equals(message.getType())) {
            rabbitTemplate.convertAndSend(
                    "chat.exchange",
                    "chat.message.sent",
                    message
            );

            log.info("Message forwarded to RabbitMQ");
        } else {
            log.info("Typing indicator routed");
        }
    }
}
