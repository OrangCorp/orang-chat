package com.orang.messageservice.service;

import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.DirectConversationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConversationNotificationEventServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ConversationNotificationEventService service;

    @BeforeEach
    void setUp() {
        service = new ConversationNotificationEventService(rabbitTemplate);
    }

    @Test
    void directConversationCreatedPublishesExpectedEvent() {
        UUID conversationId = UUID.randomUUID();
        UUID initiatorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        service.directConversationCreated(conversationId, initiatorId, recipientId);

        ArgumentCaptor<DirectConversationCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DirectConversationCreatedEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq("chat.exchange"),
                eq(RabbitMQConstants.DIRECT_CONVERSATION_CREATED_KEY),
                eventCaptor.capture()
        );

        DirectConversationCreatedEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(initiatorId, event.getInitiatorId());
        assertEquals(recipientId, event.getRecipientId());
    }
}
