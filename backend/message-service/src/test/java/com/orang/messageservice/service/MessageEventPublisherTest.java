package com.orang.messageservice.service;

import com.orang.messageservice.entity.ReactionType;
import com.orang.shared.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MessageEventPublisher messageEventPublisher;
    private UUID messageId;
    private UUID conversationId;
    private UUID userId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        messageEventPublisher = new MessageEventPublisher(rabbitTemplate);
        messageId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        authorId = UUID.randomUUID();
    }

    // ============ publishMessageEdited Tests ============

    @Test
    void publishMessageEditedPublishesEvent() {
        String newContent = "Edited content";
        LocalDateTime editedAt = LocalDateTime.now();

        messageEventPublisher.publishMessageEdited(messageId, conversationId, userId, newContent, editedAt);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageEditedEvent> eventCaptor = ArgumentCaptor.forClass(MessageEditedEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("chat.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("message.edited", routingKeyCaptor.getValue());
        MessageEditedEvent event = eventCaptor.getValue();
        assertEquals(messageId, event.getMessageId());
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getTriggeredBy());
        assertEquals(newContent, event.getNewContent());
    }

    // ============ publishMessageDeleted Tests ============

    @Test
    void publishMessageDeletedPublishesEvent() {
        LocalDateTime deletedAt = LocalDateTime.now();

        messageEventPublisher.publishMessageDeleted(messageId, conversationId, userId, deletedAt);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MessageDeletedEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("chat.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("message.deleted", routingKeyCaptor.getValue());
        MessageDeletedEvent event = eventCaptor.getValue();
        assertEquals(messageId, event.getMessageId());
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getTriggeredBy());
        assertEquals(userId, event.getDeletedBy());
    }

    // ============ publishReactionChanged Tests ============

    @Test
    void publishReactionChangedPublishesAddedAction() {
        Map<ReactionType, Long> reactionCounts = new HashMap<>();
        reactionCounts.put(ReactionType.HEART, 5L);
        reactionCounts.put(ReactionType.LIKE, 3L);

        messageEventPublisher.publishReactionChanged(
                messageId, conversationId, userId,
                MessageReactionEvent.Action.ADDED,
                ReactionType.HEART,
                reactionCounts,
                authorId
        );

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageReactionEvent> eventCaptor = ArgumentCaptor.forClass(MessageReactionEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("chat.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("message.reaction", routingKeyCaptor.getValue());
        MessageReactionEvent event = eventCaptor.getValue();
        assertEquals(messageId, event.getMessageId());
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getTriggeredBy());
        assertEquals(MessageReactionEvent.Action.ADDED, event.getAction());
        assertEquals("HEART", event.getReactionType());
        assertTrue(event.getCurrentCounts().containsKey("HEART"));
        assertEquals(5L, event.getCurrentCounts().get("HEART"));
    }

    @Test
    void publishReactionChangedConvertReactionTypesToStrings() {
        Map<ReactionType, Long> reactionCounts = new HashMap<>();
        reactionCounts.put(ReactionType.LIKE, 1L);
        reactionCounts.put(ReactionType.LAUGH, 2L);

        messageEventPublisher.publishReactionChanged(
                messageId, conversationId, userId,
                MessageReactionEvent.Action.CHANGED,
                ReactionType.LAUGH,
                reactionCounts,
                authorId
        );

        ArgumentCaptor<MessageReactionEvent> eventCaptor = ArgumentCaptor.forClass(MessageReactionEvent.class);
        verify(rabbitTemplate).convertAndSend(any(), any(), eventCaptor.capture());

        MessageReactionEvent event = eventCaptor.getValue();
        assertTrue(event.getCurrentCounts().containsKey("LIKE"));
        assertTrue(event.getCurrentCounts().containsKey("LAUGH"));
        assertTrue(event.getCurrentCounts().values().stream().allMatch(v -> v instanceof Long));
    }

    // ============ publishMessagePinChanged Tests ============

    @Test
    void publishMessagePinChangedPublishesPinnedEvent() {
        messageEventPublisher.publishMessagePinChanged(
                messageId, conversationId, userId, MessagePinnedEvent.Action.PINNED);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePinnedEvent> eventCaptor = ArgumentCaptor.forClass(MessagePinnedEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("chat.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("message.pin", routingKeyCaptor.getValue());
        MessagePinnedEvent event = eventCaptor.getValue();
        assertEquals(messageId, event.getMessageId());
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getTriggeredBy());
        assertEquals(MessagePinnedEvent.Action.PINNED, event.getAction());
    }

    @Test
    void publishMessagePinChangedPublishesUnpinnedEvent() {
        messageEventPublisher.publishMessagePinChanged(
                messageId, conversationId, userId, MessagePinnedEvent.Action.UNPINNED);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePinnedEvent> eventCaptor = ArgumentCaptor.forClass(MessagePinnedEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("chat.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("message.pin", routingKeyCaptor.getValue());
        MessagePinnedEvent event = eventCaptor.getValue();
        assertEquals(MessagePinnedEvent.Action.UNPINNED, event.getAction());
    }


}
