package com.orang.messageservice.controller;

import com.orang.messageservice.dto.*;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.service.MessageSearchService;
import com.orang.messageservice.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageSearchService messageSearchService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageController messageController;

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
    void getChatHistoryDelegatesToService() {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPage(2);
        pageRequest.setSize(15);

        MessageResponse responseMessage = mock(MessageResponse.class);
        Page<MessageResponse> expected = new PageImpl<>(List.of(responseMessage));

        when(messageService.getMessagesForConversation(conversationId, userId, PageRequest.of(2, 15)))
                .thenReturn(expected);

        var response = messageController.getChatHistory(conversationId, userId.toString(), pageRequest);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(messageService).getMessagesForConversation(conversationId, userId, PageRequest.of(2, 15));
    }

    @Test
    void searchMessagesDelegatesToService() {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPage(1);
        pageRequest.setSize(20);

        MessageSearchResponse responseMessage = mock(MessageSearchResponse.class);
        Page<MessageSearchResponse> expected = new PageImpl<>(List.of(responseMessage));

        when(messageSearchService.searchMessages(conversationId, userId, "hello", PageRequest.of(1, 20)))
                .thenReturn(expected);

        var response = messageController.searchMessages(conversationId, userId.toString(), "hello", pageRequest);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(messageSearchService).searchMessages(conversationId, userId, "hello", PageRequest.of(1, 20));
    }

    @Test
    void getMessagesAroundDelegatesToService() {
        MessageResponse aroundMessage = mock(MessageResponse.class);

        MessagesAroundResponse expected = new MessagesAroundResponse();
        expected.setTargetMessageId(messageId);
        expected.setTargetIndex(1);
        expected.setMessages(List.of(aroundMessage));
        expected.setHasOlderMessages(true);
        expected.setHasNewerMessages(false);

        when(messageSearchService.getMessagesAround(conversationId, userId, messageId, 25))
                .thenReturn(expected);

        var response = messageController.getMessagesAround(conversationId, userId.toString(), messageId, 25);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(messageSearchService).getMessagesAround(conversationId, userId, messageId, 25);
    }

    @Test
    void sendMessageDelegatesAllFields() {
        CreateMessageRequest request = new CreateMessageRequest();
        List<UUID> attachmentIds = List.of(UUID.randomUUID());
        UUID replyToMessageId = UUID.randomUUID();
        request.setConversationId(conversationId);
        request.setContent("hello");
        request.setAttachmentIds(attachmentIds);
        request.setReplyToMessageId(replyToMessageId);
        request.setMessageId(messageId);

        MessageResponse expected = mock(MessageResponse.class);

        when(messageService.saveMessage(conversationId, userId, "hello", attachmentIds, replyToMessageId, messageId))
                .thenReturn(expected);

        var response = messageController.sendMessage(request, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(messageService).saveMessage(conversationId, userId, "hello", attachmentIds, replyToMessageId, messageId);
    }

    @Test
    void editMessageDelegatesToService() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("updated");

        MessageResponse expected = mock(MessageResponse.class);

        when(messageService.editMessage(messageId, userId, "updated")).thenReturn(expected);

        var response = messageController.editMessage(messageId, request, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(messageService).editMessage(messageId, userId, "updated");
    }

    @Test
    void deleteMessageDelegatesToService() {
        var response = messageController.deleteMessage(messageId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(messageService).deleteMessage(messageId, userId);
    }

    @Test
    void getMyMentionsMapsMessagesAndCapsPageSize() {
        Message message = new Message();
        message.setId(messageId);
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setContent("mention");

        MessageResponse mapped = mock(MessageResponse.class);

        when(messageRepository.findMentionedMessages(userId, conversationId, PageRequest.of(3, 50)))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(messageMapper.toMessageResponse(any(Message.class), any(UUID.class))).thenReturn(mapped);

        var response = messageController.getMyMentions(conversationId, 3, 100, userId.toString());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(mapped);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findMentionedMessages(eq(userId), eq(conversationId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(3, 50));
        verify(messageMapper).toMessageResponse(any(Message.class), any(UUID.class));
    }
}
