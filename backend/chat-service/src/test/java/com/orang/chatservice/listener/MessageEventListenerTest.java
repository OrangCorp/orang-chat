package com.orang.chatservice.listener;

import com.orang.shared.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageEventListener messageEventListener;

    private UUID messageId;
    private UUID conversationId;
    private UUID triggeredBy;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        triggeredBy = UUID.randomUUID();
    }

    @Test
    void handleMessageEditedBroadcastsToConversationTopic() {
        MessageEditedEvent event = MessageEditedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .newContent("Updated content")
                .editedAt(LocalDateTime.now())
                .build();

        messageEventListener.handleMessageEdited(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                event
        );
    }

    @Test
    void handleMessageDeletedBroadcastsToConversationTopic() {
        MessageDeletedEvent event = MessageDeletedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .build();

        messageEventListener.handleMessageDeleted(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                event
        );
    }

    @Test
    void handleMessageReactionBroadcastsToConversationTopic() {
        MessageReactionEvent event = MessageReactionEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .reactionType("HEART")
                .action(MessageReactionEvent.Action.ADDED)
                .build();

        messageEventListener.handleMessageReaction(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                event
        );
    }

    @Test
    void handleMessagePinBroadcastsToConversationTopic() {
        MessagePinnedEvent event = MessagePinnedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .action(MessagePinnedEvent.Action.PINNED)
                .build();

        messageEventListener.handleMessagePin(event);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                event
        );
    }
}
