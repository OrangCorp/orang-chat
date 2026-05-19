package com.orang.messageservice.controller;

import com.orang.messageservice.dto.PinnedMessagesResponse;
import com.orang.messageservice.service.PinnedMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinnedMessageControllerTest {

    @Mock
    private PinnedMessageService pinnedMessageService;

    @InjectMocks
    private PinnedMessageController pinnedMessageController;

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
    void pinMessageDelegatesToService() {
        var response = pinnedMessageController.pinMessage(conversationId, messageId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(pinnedMessageService).pinMessage(conversationId, messageId, userId);
    }

    @Test
    void unpinMessageDelegatesToService() {
        var response = pinnedMessageController.unpinMessage(conversationId, messageId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(pinnedMessageService).unpinMessage(conversationId, messageId, userId);
    }

    @Test
    void getPinnedMessagesBuildsResponse() {
        List<UUID> pinnedIds = List.of(messageId);
        when(pinnedMessageService.getPinnedMessageIds(conversationId, userId)).thenReturn(pinnedIds);

        PinnedMessagesResponse expected = new PinnedMessagesResponse();
        expected.setConversationId(conversationId);
        expected.setPinnedMessageIds(pinnedIds);
        expected.setCount(1);

        var response = pinnedMessageController.getPinnedMessages(conversationId, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(pinnedMessageService).getPinnedMessageIds(conversationId, userId);
    }

    @Test
    void isPinnedDelegatesToService() {
        when(pinnedMessageService.isPinned(conversationId, messageId)).thenReturn(true);

        var response = pinnedMessageController.isPinned(conversationId, messageId);

        assertThat(response.getBody()).isEqualTo(true);
        verify(pinnedMessageService).isPinned(conversationId, messageId);
    }
}
