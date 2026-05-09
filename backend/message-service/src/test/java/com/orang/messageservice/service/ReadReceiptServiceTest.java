package com.orang.messageservice.service;

import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.ReadReceipt;
import com.orang.messageservice.entity.ReadReceiptId;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.ReadReceiptRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadReceiptServiceTest {

    @Mock
    private ReadReceiptRepository readReceiptRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    private ReadReceiptService readReceiptService;
    private UUID conversationId;
    private UUID userId;
    private UUID messageId;

    @BeforeEach
    void setUp() {
        readReceiptService = new ReadReceiptService(readReceiptRepository, messageRepository, conversationService);
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        messageId = UUID.randomUUID();
    }

    // ============ markAsRead Tests ============

    @Test
    void markAsReadThrowsWhenUserNotParticipant() {
        doThrow(new ResourceNotFoundException("Not a participant"))
                .when(conversationService).verifyParticipant(conversationId, userId);

        assertThrows(ResourceNotFoundException.class,
                () -> readReceiptService.markAsRead(userId, conversationId, messageId));

        verify(messageRepository, never()).findActiveById(any());
    }

    @Test
    void markAsReadThrowsWhenMessageNotFound() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> readReceiptService.markAsRead(userId, conversationId, messageId));
    }

    @Test
    void markAsReadThrowsWhenMessageNotInConversation() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID otherConversationId = UUID.randomUUID();
        Message message = buildMessage(messageId, otherConversationId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class,
                () -> readReceiptService.markAsRead(userId, conversationId, messageId));
    }

    @Test
    void markAsReadCreatesNewReceiptWhenNoneExists() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        Message message = buildMessage(messageId, conversationId);
        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(message));
        when(readReceiptRepository.findById(any()))
                .thenReturn(Optional.empty());
        when(readReceiptRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        readReceiptService.markAsRead(userId, conversationId, messageId);

        ArgumentCaptor<ReadReceipt> captor = ArgumentCaptor.forClass(ReadReceipt.class);
        verify(readReceiptRepository).save(captor.capture());

        ReadReceipt saved = captor.getValue();
        assertEquals(userId, saved.getId().getUserId());
        assertEquals(conversationId, saved.getId().getConversationId());
        assertEquals(messageId, saved.getLastReadMessageId());
        assertNotNull(saved.getReadAt());
    }

    @Test
    void markAsReadUpdatesExistingReceiptWhenNewer() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID oldMessageId = UUID.randomUUID();
        LocalDateTime oldTime = LocalDateTime.now().minusHours(1);
        LocalDateTime newTime = LocalDateTime.now();

        Message oldMessage = buildMessage(oldMessageId, conversationId, oldTime);
        Message newMessage = buildMessage(messageId, conversationId, newTime);

        ReadReceipt existing = ReadReceipt.builder()
                .id(ReadReceiptId.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build())
                .lastReadMessageId(oldMessageId)
                .readAt(LocalDateTime.now())
                .build();

        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(newMessage));
        when(readReceiptRepository.findById(any()))
                .thenReturn(Optional.of(existing));
        when(messageRepository.findById(oldMessageId))
                .thenReturn(Optional.of(oldMessage));

        readReceiptService.markAsRead(userId, conversationId, messageId);

        verify(readReceiptRepository).save(argThat(receipt ->
                receipt.getLastReadMessageId().equals(messageId)));
    }

    @Test
    void markAsReadIgnoresOlderMessage() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID oldMessageId = UUID.randomUUID();
        LocalDateTime newTime = LocalDateTime.now();
        LocalDateTime oldTime = LocalDateTime.now().minusHours(1);

        Message newMessage = buildMessage(messageId, conversationId, oldTime);
        Message oldMessage = buildMessage(oldMessageId, conversationId, newTime);

        ReadReceipt existing = ReadReceipt.builder()
                .id(ReadReceiptId.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build())
                .lastReadMessageId(oldMessageId)
                .readAt(LocalDateTime.now())
                .build();

        when(messageRepository.findActiveById(messageId))
                .thenReturn(Optional.of(newMessage));
        when(readReceiptRepository.findById(any()))
                .thenReturn(Optional.of(existing));
        when(messageRepository.findById(oldMessageId))
                .thenReturn(Optional.of(oldMessage));

        readReceiptService.markAsRead(userId, conversationId, messageId);

        verify(readReceiptRepository, never()).save(any());
    }

    // ============ getUnreadCount Tests ============

    @Test
    void getUnreadCountReturnsZeroWhenNoReceipt() {
        when(readReceiptRepository.findById(any()))
                .thenReturn(Optional.empty());
        when(messageRepository.countActiveMessagesByConversationId(conversationId))
                .thenReturn(0L);

        long count = readReceiptService.getUnreadCount(userId, conversationId);

        assertEquals(0, count);
    }

    @Test
    void getUnreadCountReturnsCountOfMessagesAfterLastRead() {
        ReadReceipt receipt = ReadReceipt.builder()
                .id(ReadReceiptId.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build())
                .lastReadMessageId(messageId)
                .readAt(LocalDateTime.now())
                .build();

        when(readReceiptRepository.findById(any()))
                .thenReturn(Optional.of(receipt));
        when(messageRepository.countUnreadMessages(conversationId, messageId))
                .thenReturn(5L);

        long count = readReceiptService.getUnreadCount(userId, conversationId);

        assertEquals(5, count);
    }

    // ============ Helper Methods ============

    private Message buildMessage(UUID messageId, UUID conversationId) {
        return buildMessage(messageId, conversationId, LocalDateTime.now());
    }

    private Message buildMessage(UUID messageId, UUID conversationId, LocalDateTime createdAt) {
        return Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(UUID.randomUUID())
                .content("Test message")
                .createdAt(createdAt)
                .build();
    }
}
