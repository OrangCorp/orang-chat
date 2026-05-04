package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageSearchResponse;
import com.orang.messageservice.dto.MessagesAroundResponse;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.MessageRepositoryProjection;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSearchServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageMapper messageMapper;

    private MessageSearchService messageSearchService;
    private UUID conversationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        messageSearchService = new MessageSearchService(messageRepository, conversationService, messageMapper);
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ============ searchMessages Tests ============

    @Test
    void searchMessagesThrowsWhenQueryTooShort() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(BadRequestException.class,
                () -> messageSearchService.searchMessages(conversationId, userId, "a", pageable));

        verifyNoInteractions(messageRepository);
    }

    @Test
    void searchMessagesThrowsWhenQueryTooShortWithSpaces() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(BadRequestException.class,
                () -> messageSearchService.searchMessages(conversationId, userId, "  ", pageable));

        verifyNoInteractions(messageRepository);
    }

    @Test
    void searchMessagesThrowsWhenUserNotParticipant() {
        doThrow(new ResourceNotFoundException("Not a participant"))
                .when(conversationService).verifyParticipant(conversationId, userId);

        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(ResourceNotFoundException.class,
                () -> messageSearchService.searchMessages(conversationId, userId, "test search", pageable));

        verifyNoInteractions(messageRepository);
    }

    @Test
    void searchMessagesReturnsResultsForValidQuery() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        MessageRepositoryProjection projection1 = mock(MessageRepositoryProjection.class);
        MessageRepositoryProjection projection2 = mock(MessageRepositoryProjection.class);

        Page<MessageRepositoryProjection> mockPage = new PageImpl<>(
                Arrays.asList(projection1, projection2),
                PageRequest.of(0, 20),
                2
        );

        when(messageRepository.searchMessages(conversationId, "hello world", PageRequest.of(0, 20)))
                .thenReturn(mockPage);

        Pageable pageable = PageRequest.of(0, 20);
        Page<MessageSearchResponse> result = messageSearchService.searchMessages(
                conversationId, userId, "hello world", pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(messageRepository).searchMessages(conversationId, "hello world", PageRequest.of(0, 20));
    }

    @Test
    void searchMessagesClampPageSizeToMax() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        when(messageRepository.searchMessages(any(), any(), any()))
                .thenReturn(new PageImpl<>(Arrays.asList()));

        Pageable oversizedPageable = PageRequest.of(0, 100); // Larger than MAX_PAGE_SIZE
        messageSearchService.searchMessages(conversationId, userId, "test", oversizedPageable);

        verify(messageRepository).searchMessages(any(), any(), argThat(p -> p.getPageSize() <= 50));
    }

    // ============ getMessagesAround Tests ============

    @Test
    void getMessagesAroundThrowsWhenUserNotParticipant() {
        doThrow(new ResourceNotFoundException("Not a participant"))
                .when(conversationService).verifyParticipant(conversationId, userId);

        UUID messageId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class,
                () -> messageSearchService.getMessagesAround(conversationId, userId, messageId, 5));

        verify(messageRepository, never()).findById(any());
    }

    @Test
    void getMessagesAroundThrowsWhenMessageNotFound() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messageSearchService.getMessagesAround(conversationId, userId, messageId, 5));
    }

    @Test
    void getMessagesAroundThrowsWhenMessageNotInConversation() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID messageId = UUID.randomUUID();
        UUID otherConversationId = UUID.randomUUID();

        Message message = Message.builder()
                .id(messageId)
                .conversationId(otherConversationId)
                .senderId(UUID.randomUUID())
                .content("Test")
                .build();

        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class,
                () -> messageSearchService.getMessagesAround(conversationId, userId, messageId, 5));
    }

    @Test
    void getMessagesAroundReturnsMessagesAroundTarget() {
        doNothing().when(conversationService).verifyParticipant(conversationId, userId);

        UUID messageId = UUID.randomUUID();
        Message targetMessage = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(UUID.randomUUID())
                .content("Target message")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(targetMessage));

        Message before = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(UUID.randomUUID())
                .content("Before")
                .createdAt(java.time.LocalDateTime.now().minusMinutes(10))
                .build();

        Message after = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(UUID.randomUUID())
                .content("After")
                .createdAt(java.time.LocalDateTime.now().plusMinutes(10))
                .build();

        when(messageRepository.findMessagesBeforeTimestamp(conversationId, targetMessage.getCreatedAt(), PageRequest.of(0, 2)))
                .thenReturn(Arrays.asList(before));
        when(messageRepository.findMessagesAfterTimestamp(conversationId, targetMessage.getCreatedAt(), PageRequest.of(0, 2)))
                .thenReturn(Arrays.asList(after));

        MessagesAroundResponse result = messageSearchService.getMessagesAround(
                conversationId, userId, messageId, 5);

        assertNotNull(result);
        assertEquals(messageId, result.getTargetMessageId());
        assertEquals(3, result.getMessages().size()); // before + target + after
        assertTrue(result.getHasOlderMessages());
        assertTrue(result.getHasNewerMessages());
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
