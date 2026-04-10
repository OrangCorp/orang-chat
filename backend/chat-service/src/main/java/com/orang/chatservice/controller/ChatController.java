package com.orang.chatservice.controller;

import com.orang.shared.dto.ChatMessagePayload;
import com.orang.shared.dto.MessageType;
import com.orang.chatservice.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;
    private final PresenceService presenceService;

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessagePayload message) {
        if (!isValidPayload(message)) {
            return;
        }

        log.info("Received {} message from {} to {}",
                message.getType() ,
                message.getSenderId(),
                message.getRecipientId());

        if (MessageType.DIRECT.equals(message.getType()) && message.getSenderId().equals(message.getRecipientId())) {
            throw new IllegalArgumentException("You cannot send a message to yourself");
        }

        presenceService.updateLastActivity(message.getSenderId().toString());

        message.setTimestamp(LocalDateTime.now());

        if (MessageType.GROUP.equals(message.getType())) {
            messagingTemplate.convertAndSend(
                    "/topic/group." + message.getConversationId(),
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

    private boolean isValidPayload(ChatMessagePayload message) {
        if (message == null) {
            log.warn("Dropping websocket message: payload is null");
            return false;
        }

        if (message.getType() == null) {
            log.warn("Dropping websocket message: type is required");
            return false;
        }

        if (message.getSenderId() == null) {
            log.warn("Dropping websocket message: senderId is required for type {}", message.getType());
            return false;
        }

        if (MessageType.GROUP.equals(message.getType()) && message.getConversationId() == null) {
            log.warn("Dropping websocket GROUP message: conversationId is required");
            return false;
        }

        if (!MessageType.GROUP.equals(message.getType()) && message.getRecipientId() == null) {
            log.warn("Dropping websocket {} message: recipientId is required", message.getType());
            return false;
        }

        return true;
    }
}
