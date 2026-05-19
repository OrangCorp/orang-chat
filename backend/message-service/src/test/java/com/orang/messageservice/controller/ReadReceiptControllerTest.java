package com.orang.messageservice.controller;

import com.orang.messageservice.dto.ReadReceiptResponse;
import com.orang.messageservice.service.ReadReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadReceiptControllerTest {

    @Mock
    private ReadReceiptService readReceiptService;

    @InjectMocks
    private ReadReceiptController readReceiptController;

    private UUID conversationId;
    private UUID messageId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void markAsReadDelegatesToService() {
        var response = readReceiptController.markAsRead(conversationId, messageId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(readReceiptService).markAsRead(userId, conversationId, messageId);
    }

    @Test
    void getUnreadCountDelegatesToService() {
        when(readReceiptService.getUnreadCount(userId, conversationId)).thenReturn(7L);

        var response = readReceiptController.getUnreadCount(conversationId, userId.toString());

        assertThat(response.getBody()).isEqualTo(7L);
        verify(readReceiptService).getUnreadCount(userId, conversationId);
    }

    @Test
    void getReadReceiptComposesResponse() {
        when(readReceiptService.getLastReadMessageId(userId, conversationId)).thenReturn(Optional.of(messageId));
        when(readReceiptService.getUnreadCount(userId, conversationId)).thenReturn(3L);

        ReadReceiptResponse expected = new ReadReceiptResponse();
        expected.setConversationId(conversationId);
        expected.setLastReadMessageId(messageId);
        expected.setUnreadCount(3L);

        var response = readReceiptController.getReadReceipt(conversationId, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(readReceiptService).getLastReadMessageId(userId, conversationId);
        verify(readReceiptService).getUnreadCount(userId, conversationId);
    }
}
