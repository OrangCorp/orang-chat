package com.orang.messageservice.controller;

import com.orang.messageservice.dto.*;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    private UUID conversationId;
    private UUID userId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    @Test
    void getConversationsDelegatesToService() {
        ConversationResponse expectedConversation = new ConversationResponse();
        expectedConversation.setId(conversationId);
        List<ConversationResponse> expected = List.of(expectedConversation);
        when(conversationService.getConversations(userId)).thenReturn(expected);

        var response = conversationController.getConversations(userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(conversationService).getConversations(userId);
    }

    @Test
    void getOrCreateDirectChatDelegatesToService() {
        ConversationResponse expected = new ConversationResponse();
        expected.setId(conversationId);
        expected.setType(Conversation.ConversationType.DIRECT);
        when(conversationService.getOrCreateDirectConversation(userId, targetUserId)).thenReturn(expected);

        var response = conversationController.getOrCreateDirectChat(userId.toString(), targetUserId);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(conversationService).getOrCreateDirectConversation(userId, targetUserId);
    }

    @Test
    void createGroupChatReturnsCreated() {
        Set<UUID> participantIds = Set.of(targetUserId);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Team");
        request.setParticipantIds(participantIds);
        ConversationResponse expected = new ConversationResponse();
        expected.setId(conversationId);
        expected.setType(Conversation.ConversationType.GROUP);
        when(conversationService.createGroupConversation("Team", participantIds, userId)).thenReturn(expected);

        var response = conversationController.createGroupChat(userId.toString(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(conversationService).createGroupConversation("Team", participantIds, userId);
    }

    @Test
    void addParticipantsDelegatesToService() {
        Set<UUID> userIds = Set.of(targetUserId);
        AddParticipantsRequest request = new AddParticipantsRequest();
        request.setUserIds(userIds);
        ConversationResponse expected = new ConversationResponse();
        expected.setId(conversationId);
        when(conversationService.addParticipants(conversationId, userIds, userId)).thenReturn(expected);

        var response = conversationController.addParticipants(userId.toString(), conversationId, request);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(conversationService).addParticipants(conversationId, userIds, userId);
    }

    @Test
    void removeParticipantDelegatesToService() {
        var response = conversationController.removeParticipant(conversationId, targetUserId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).removeParticipant(conversationId, targetUserId, userId);
    }

    @Test
    void leaveConversationDelegatesToService() {
        var response = conversationController.leaveConversation(conversationId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).leaveConversation(conversationId, userId);
    }

    @Test
    void renameConversationDelegatesToService() {
        RenameConversationRequest request = new RenameConversationRequest();
        request.setName("Renamed");
        ConversationResponse expected = new ConversationResponse();
        expected.setId(conversationId);
        expected.setName("Renamed");
        when(conversationService.renameConversation(conversationId, "Renamed", userId)).thenReturn(expected);

        var response = conversationController.renameConversation(conversationId, request, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(conversationService).renameConversation(conversationId, "Renamed", userId);
    }

    @Test
    void promoteAndDemoteDelegatesToService() {
        ConversationResponse promoted = new ConversationResponse();
        promoted.setId(conversationId);
        ConversationResponse demoted = new ConversationResponse();
        demoted.setId(conversationId);
        when(conversationService.promoteParticipant(conversationId, targetUserId, userId)).thenReturn(promoted);
        when(conversationService.demoteParticipant(conversationId, targetUserId, userId)).thenReturn(demoted);

        assertThat(conversationController.promoteParticipant(conversationId, targetUserId, userId.toString()).getBody())
                .isEqualTo(promoted);
        assertThat(conversationController.demoteParticipant(conversationId, targetUserId, userId.toString()).getBody())
                .isEqualTo(demoted);

        verify(conversationService).promoteParticipant(conversationId, targetUserId, userId);
        verify(conversationService).demoteParticipant(conversationId, targetUserId, userId);
    }

    @Test
    void deleteConversationDelegatesToService() {
        var response = conversationController.deleteConversation(conversationId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).deleteConversation(conversationId, userId);
    }
}
