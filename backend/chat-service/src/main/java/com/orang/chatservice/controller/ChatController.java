package com.orang.chatservice.controller;

import com.orang.chatservice.dto.ChatMessage;
import com.orang.chatservice.dto.MessageType;
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

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage message) {
        log.info("Received message from {} to {}", message.getSenderId(), message.getRecipientId());

        if (message.getSenderId().equals(message.getRecipientId())) {
            throw new IllegalArgumentException("You send message to yourself");
        }

        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(
                message.getRecipientId().toString(),
                "/queue/messages",
                message
        );

        if (MessageType.CHAT.equals(message.getType())) {
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
