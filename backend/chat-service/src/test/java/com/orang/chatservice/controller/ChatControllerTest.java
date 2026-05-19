package com.orang.chatservice.controller;

import com.orang.shared.dto.ChatMessagePayload;
import com.orang.shared.dto.MessageType;
import com.orang.chatservice.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private ChatController chatController;

    private ChatMessagePayload directMessage;
    private ChatMessagePayload groupMessage;
    private UUID senderId;
    private UUID recipientId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        directMessage = new ChatMessagePayload();
        directMessage.setSenderId(senderId);
        directMessage.setRecipientId(recipientId);
        directMessage.setType(MessageType.DIRECT);
        directMessage.setContent("Hello");

        groupMessage = new ChatMessagePayload();
        groupMessage.setSenderId(senderId);
        groupMessage.setConversationId(conversationId);
        groupMessage.setType(MessageType.GROUP);
        groupMessage.setContent("Group message");
    }

    @Test
    void processMessageRoutesDirectMessageToUser() {
        chatController.processMessage(directMessage);

        verify(messagingTemplate).convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                directMessage
        );
        verify(messagingTemplate).convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                directMessage
        );
        verify(rabbitTemplate).convertAndSend("chat.exchange", "chat.message.sent", directMessage);
        verify(presenceService).updateLastActivity(senderId.toString());
    }

    @Test
    void processMessageRoutesGroupMessageToTopic() {
        chatController.processMessage(groupMessage);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                groupMessage
        );
        verify(rabbitTemplate).convertAndSend("chat.exchange", "chat.message.sent", groupMessage);
    }

    @Test
    void processMessageSetsTimestampIfNull() {
        directMessage.setTimestamp(null);

        chatController.processMessage(directMessage);

        assertNotNull(directMessage.getTimestamp());
        assertTrue(directMessage.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void processMessageSetsMessageIdIfNull() {
        directMessage.setMessageId(null);

        chatController.processMessage(directMessage);

        assertNotNull(directMessage.getMessageId());
    }

    @Test
    void processMessageThrowsWhenSendingToSelf() {
        directMessage.setRecipientId(senderId);

        assertThrows(IllegalArgumentException.class, () -> chatController.processMessage(directMessage));
    }

    @Test
    void processMessageRouteTypingIndicatorToGroup() {
        ChatMessagePayload typingMessage = new ChatMessagePayload();
        typingMessage.setSenderId(senderId);
        typingMessage.setConversationId(conversationId);
        typingMessage.setType(MessageType.TYPING);

        chatController.processMessage(typingMessage);

        verify(messagingTemplate).convertAndSend(
                "/topic/group." + conversationId,
                typingMessage
        );
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void processMessageDropsNullPayload() {
        chatController.processMessage(null);

        verifyNoInteractions(messagingTemplate, rabbitTemplate, presenceService);
    }

    @Test
    void processMessageDropsMessageWithoutType() {
        directMessage.setType(null);

        chatController.processMessage(directMessage);

        verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }

    @Test
    void processMessageDropsMessageWithoutSenderId() {
        directMessage.setSenderId(null);

        chatController.processMessage(directMessage);

        verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }

    @Test
    void processMessageDropsGroupMessageWithoutConversationId() {
        groupMessage.setConversationId(null);

        chatController.processMessage(groupMessage);

        verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }

    @Test
    void processMessageDropsDirectMessageWithoutRecipientId() {
        directMessage.setRecipientId(null);

        chatController.processMessage(directMessage);

        verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }

    @Test
    void processMessageDropsTypingIndicatorWithoutDestination() {
        ChatMessagePayload typingMessage = new ChatMessagePayload();
        typingMessage.setSenderId(senderId);
        typingMessage.setType(MessageType.TYPING);

        chatController.processMessage(typingMessage);

        verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }
}
