package com.orang.messageservice.service;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.ConversationParticipant;
import com.orang.messageservice.entity.ConversationParticipantId;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private GroupEventService groupEventService;

    @Mock
    private ConversationNotificationEventService conversationNotificationEventService;

    private ConversationService conversationService;
    private UUID userId1;
    private UUID userId2;
    private UUID groupAdminId;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                groupEventService,
                conversationNotificationEventService
        );
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        groupAdminId = UUID.randomUUID();
    }

    // ============ getConversations Tests ============

    @Test
    void getConversationsReturnsEmptyListWhenUserHasNoConversations() {
        when(conversationRepository.findByParticipantIdsContaining(userId1))
                .thenReturn(Collections.emptyList());

        var result = conversationService.getConversations(userId1);

        assertTrue(result.isEmpty());
        verify(conversationRepository).findByParticipantIdsContaining(userId1);
    }

    @Test
    void getConversationsReturnsMultipleConversationsForUser() {
        Conversation conv1 = buildConversation(Conversation.ConversationType.DIRECT);
        Conversation conv2 = buildConversation(Conversation.ConversationType.GROUP, "Group Chat");

        when(conversationRepository.findByParticipantIdsContaining(userId1))
                .thenReturn(Arrays.asList(conv1, conv2));

        var result = conversationService.getConversations(userId1);

        assertEquals(2, result.size());
        verify(conversationRepository).findByParticipantIdsContaining(userId1);
    }

    // ============ getOrCreateDirectConversation Tests ============

    @Test
    void getOrCreateDirectConversationThrowsWhenUsersAreSame() {
        assertThrows(IllegalArgumentException.class,
                () -> conversationService.getOrCreateDirectConversation(userId1, userId1));

        verify(conversationRepository, never()).findDirectConversationBetween(any(), any());
    }

    @Test
    void getOrCreateDirectConversationReturnsExistingConversation() {
        Conversation existing = buildConversation(Conversation.ConversationType.DIRECT);
        when(conversationRepository.findDirectConversationBetween(userId1, userId2))
                .thenReturn(Optional.of(existing));

        var result = conversationService.getOrCreateDirectConversation(userId1, userId2);

        assertNotNull(result);
        verify(conversationRepository).findDirectConversationBetween(userId1, userId2);
        verify(conversationRepository, never()).save(any());
        verify(conversationNotificationEventService, never())
            .directConversationCreated(any(), any(), any());
    }

    @Test
    void getOrCreateDirectConversationCreatesNewWhenNotExists() {
        when(conversationRepository.findDirectConversationBetween(userId1, userId2))
                .thenReturn(Optional.empty());

        Conversation saved = buildConversation(Conversation.ConversationType.DIRECT);
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(saved);

        var result = conversationService.getOrCreateDirectConversation(userId1, userId2);

        assertNotNull(result);
        verify(conversationRepository).save(argThat(conv ->
                conv.getType() == Conversation.ConversationType.DIRECT &&
                        conv.getParticipants().size() == 2
        ));
        verify(conversationNotificationEventService)
            .directConversationCreated(saved.getId(), userId1, userId2);
    }

    // ============ createGroupConversation Tests ============

    @Test
    void createGroupConversationThrowsWhenLessThan3Participants() {
        Set<UUID> participants = new HashSet<>();
        participants.add(userId1);

        assertThrows(BadRequestException.class,
                () -> conversationService.createGroupConversation("Group", participants, groupAdminId));

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createGroupConversationThrowsWhenExactly2Participants() {
        Set<UUID> participants = new HashSet<>();
        participants.add(userId1);
        // groupAdminId will be added, making 2 total - still not enough

        assertThrows(BadRequestException.class,
                () -> conversationService.createGroupConversation("Group", participants, groupAdminId));
    }

    @Test
    void createGroupConversationCreatesWithValidParticipants() {
        Set<UUID> participants = new HashSet<>();
        participants.add(userId1);
        participants.add(userId2);

        Conversation saved = buildConversation(Conversation.ConversationType.GROUP, "Test Group");
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(saved);

        var result = conversationService.createGroupConversation("Test Group", participants, groupAdminId);

        assertNotNull(result);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());

        Conversation created = captor.getValue();
        assertEquals(Conversation.ConversationType.GROUP, created.getType());
        assertEquals("Test Group", created.getName());
        assertEquals(groupAdminId, created.getCreatedBy());
        assertEquals(3, created.getParticipants().size()); // admin + 2 members
        verify(groupEventService).memberAdded(saved.getId(), userId1, groupAdminId);
        verify(groupEventService).memberAdded(saved.getId(), userId2, groupAdminId);
    }

    @Test
    void createGroupConversationMakesCreatorAdmin() {
        Set<UUID> participants = new HashSet<>();
        participants.add(userId1);
        participants.add(userId2);

        Conversation saved = buildConversation(Conversation.ConversationType.GROUP, "Test Group");
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(saved);

        conversationService.createGroupConversation("Test Group", participants, groupAdminId);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());

        Conversation created = captor.getValue();
        var adminParticipant = created.getParticipants().stream()
                .filter(p -> groupAdminId.equals(p.getId().getUserId()))
                .findFirst();

        assertTrue(adminParticipant.isPresent());
        assertEquals(ConversationParticipant.ParticipantRole.ADMIN, adminParticipant.get().getRole());
    }

    // ============ Helper Methods ============

    private Conversation buildConversation(Conversation.ConversationType type) {
        return buildConversation(type, null);
    }

    private Conversation buildConversation(Conversation.ConversationType type, String name) {
        UUID conversationId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(type)
                .name(name)
                .createdBy(type == Conversation.ConversationType.GROUP ? UUID.randomUUID() : null)
                .createdAt(LocalDateTime.now())
                .build();

        Set<ConversationParticipant> participants = new HashSet<>();
        if (type == Conversation.ConversationType.DIRECT) {
            participants.add(buildParticipant(conv, userId1, ConversationParticipant.ParticipantRole.MEMBER));
            participants.add(buildParticipant(conv, userId2, ConversationParticipant.ParticipantRole.MEMBER));
        } else {
            participants.add(buildParticipant(conv, groupAdminId, ConversationParticipant.ParticipantRole.ADMIN));
            participants.add(buildParticipant(conv, userId1, ConversationParticipant.ParticipantRole.MEMBER));
        }
        conv.setParticipants(participants);

        return conv;
    }

    private ConversationParticipant buildParticipant(Conversation conversation, UUID userId,
                                                      ConversationParticipant.ParticipantRole role) {
        return ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversation.getId(), userId))
                .conversation(conversation)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
