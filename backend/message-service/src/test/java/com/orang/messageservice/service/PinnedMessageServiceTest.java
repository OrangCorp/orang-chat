package com.orang.messageservice.service;

import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.PinnedMessage;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.PinnedMessageRepository;
import com.orang.shared.event.MessagePinnedEvent;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinnedMessageServiceTest {

    @Mock
    private PinnedMessageRepository pinnedMessageRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    private PinnedMessageService pinnedMessageService;
    private UUID conversationId;
    private UUID messageId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        pinnedMessageService = new PinnedMessageService(
                pinnedMessageRepository, messageRepository, conversationService, messageEventPublisher);
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ============ pinMessage Tests ============

    @Test
    void pinMessageThrowsWhenUserNotParticipant() {
        doThrow(new ResourceNotFoundException("Not a participant"))
                .when(conversationService).verifyParticipant(conversationId, userId);

        assertThrows(ResourceNotFoundException.class,
                () -> pinnedMessageService.pinMessage(conversationId, messageId, userId));

        verify(messageRepository, never()).findActiveById(any());
    }

    @Test
    void pinMessageThrowsWhenMessageNotFound() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pinnedMessageService.pinMessage(conversationId, messageId, userId));
    }

    @Test
    void pinMessageThrowsWhenMessageNotInConversation() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID otherConversationId = UUID.randomUUID();
        Message message = buildMessage(messageId, otherConversationId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class,
                () -> pinnedMessageService.pinMessage(conversationId, messageId, userId));

        verify(pinnedMessageRepository, never()).save(any());
    }

    @Test
    void pinMessageIgnoresWhenAlreadyPinned() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        Message message = buildMessage(messageId, conversationId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(message));
        when(pinnedMessageRepository.existsByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(true);

        pinnedMessageService.pinMessage(conversationId, messageId, userId);

        verify(pinnedMessageRepository, never()).save(any());
        verify(messageEventPublisher, never()).publishMessagePinChanged(any(), any(), any(), any());
    }

    @Test
    void pinMessageSuccessfullyPinsNewMessage() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        Message message = buildMessage(messageId, conversationId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(message));
        when(pinnedMessageRepository.existsByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(false);
        when(pinnedMessageRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        pinnedMessageService.pinMessage(conversationId, messageId, userId);

        verify(pinnedMessageRepository).save(argThat(pin ->
                pin.getConversationId().equals(conversationId) &&
                        pin.getMessageId().equals(messageId) &&
                        pin.getPinnedBy().equals(userId)));
        verify(messageEventPublisher).publishMessagePinChanged(messageId, conversationId, userId, MessagePinnedEvent.Action.PINNED);
    }

    // ============ unpinMessage Tests ============

    @Test
    void unpinMessageThrowsWhenNotPinned() {
        when(pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pinnedMessageService.unpinMessage(conversationId, messageId, userId));

        verify(pinnedMessageRepository, never()).delete(any());
    }

    @Test
    void unpinMessageSucceedsWhenRequeserIsPinner() {
        PinnedMessage pinnedMessage = PinnedMessage.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .pinnedBy(userId)
                .build();

        when(pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(Optional.of(pinnedMessage));

        pinnedMessageService.unpinMessage(conversationId, messageId, userId);

        verify(pinnedMessageRepository).delete(pinnedMessage);
        verify(messageEventPublisher).publishMessagePinChanged(messageId, conversationId, userId, MessagePinnedEvent.Action.UNPINNED);
    }

    @Test
    void unpinMessageSucceedsWhenRequesterIsAdmin() {
        UUID pinnerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        PinnedMessage pinnedMessage = PinnedMessage.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .pinnedBy(pinnerId)
                .build();

        when(pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(Optional.of(pinnedMessage));
        when(conversationService.isAdmin(conversationId, adminId))
                .thenReturn(true);

        pinnedMessageService.unpinMessage(conversationId, messageId, adminId);

        verify(pinnedMessageRepository).delete(pinnedMessage);
        verify(messageEventPublisher).publishMessagePinChanged(messageId, conversationId, adminId, MessagePinnedEvent.Action.UNPINNED);
    }

    @Test
    void unpinMessageThrowsWhenRequesterNeitherPinnerNorAdmin() {
        UUID pinnerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        PinnedMessage pinnedMessage = PinnedMessage.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .pinnedBy(pinnerId)
                .build();

        when(pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId))
                .thenReturn(Optional.of(pinnedMessage));
        when(conversationService.isAdmin(conversationId, otherUserId))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> pinnedMessageService.unpinMessage(conversationId, messageId, otherUserId));

        verify(pinnedMessageRepository, never()).delete(any());
    }

    // ============ Helper Methods ============

    private Message buildMessage(UUID messageId, UUID conversationId) {
        return Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(UUID.randomUUID())
                .content("Test message")
                .build();
    }
}
