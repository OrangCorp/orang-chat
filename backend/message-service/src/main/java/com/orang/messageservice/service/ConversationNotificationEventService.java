package com.orang.messageservice.service;

import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.DirectConversationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationNotificationEventService {

    private static final String CHAT_EXCHANGE = "chat.exchange";

    private final RabbitTemplate rabbitTemplate;

    public void directConversationCreated(UUID conversationId, UUID initiatorId, UUID recipientId) {
        DirectConversationCreatedEvent event = DirectConversationCreatedEvent.builder()
                .conversationId(conversationId)
                .initiatorId(initiatorId)
                .recipientId(recipientId)
                .build();

        rabbitTemplate.convertAndSend(
                CHAT_EXCHANGE,
                RabbitMQConstants.DIRECT_CONVERSATION_CREATED_KEY,
                event
        );

        log.info("Published direct conversation created event for conversation {}", conversationId);
    }
}
