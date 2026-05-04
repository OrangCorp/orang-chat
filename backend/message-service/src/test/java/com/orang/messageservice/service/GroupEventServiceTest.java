package com.orang.messageservice.service;

import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.GroupUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupEventServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private GroupEventService groupEventService;
    private UUID conversationId;
    private UUID userId;
    private UUID triggeredBy;

    @BeforeEach
    void setUp() {
        groupEventService = new GroupEventService(rabbitTemplate);
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        triggeredBy = UUID.randomUUID();
    }

    // ============ memberAdded Test ============

    @Test
    void memberAddedPublishesEvent() {
        groupEventService.memberAdded(conversationId, userId, triggeredBy);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupMemberEvent> eventCaptor = ArgumentCaptor.forClass(GroupMemberEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.member.added", routingKeyCaptor.getValue());
        GroupMemberEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getUserId());
        assertEquals(triggeredBy, event.getTriggeredBy());
        assertEquals(GroupMemberEvent.EventType.MEMBER_ADDED, event.getEventType());
    }

    // ============ memberRemoved Test ============

    @Test
    void memberRemovedPublishesEvent() {
        groupEventService.memberRemoved(conversationId, userId, triggeredBy);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupMemberEvent> eventCaptor = ArgumentCaptor.forClass(GroupMemberEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.member.removed", routingKeyCaptor.getValue());
        GroupMemberEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getUserId());
        assertEquals(triggeredBy, event.getTriggeredBy());
        assertEquals(GroupMemberEvent.EventType.MEMBER_REMOVED, event.getEventType());
    }

    // ============ memberLeft Test ============

    @Test
    void memberLeftPublishesEvent() {
        groupEventService.memberLeft(conversationId, userId);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupMemberEvent> eventCaptor = ArgumentCaptor.forClass(GroupMemberEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.member.left", routingKeyCaptor.getValue());
        GroupMemberEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getUserId());
        assertEquals(userId, event.getTriggeredBy()); // User triggered their own leave
        assertEquals(GroupMemberEvent.EventType.MEMBER_LEFT, event.getEventType());
    }

    // ============ adminPromoted Test ============

    @Test
    void adminPromotedPublishesEvent() {
        groupEventService.adminPromoted(conversationId, userId, triggeredBy);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupMemberEvent> eventCaptor = ArgumentCaptor.forClass(GroupMemberEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.admin.promoted", routingKeyCaptor.getValue());
        GroupMemberEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getUserId());
        assertEquals(triggeredBy, event.getTriggeredBy());
        assertEquals(GroupMemberEvent.EventType.ADMIN_PROMOTED, event.getEventType());
    }

    // ============ adminDemoted Test ============

    @Test
    void adminDemotedPublishesEvent() {
        groupEventService.adminDemoted(conversationId, userId, triggeredBy);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupMemberEvent> eventCaptor = ArgumentCaptor.forClass(GroupMemberEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.admin.demoted", routingKeyCaptor.getValue());
        GroupMemberEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(userId, event.getUserId());
        assertEquals(triggeredBy, event.getTriggeredBy());
        assertEquals(GroupMemberEvent.EventType.ADMIN_DEMOTED, event.getEventType());
    }

    // ============ groupRenamed Test ============

    @Test
    void groupRenamedPublishesEvent() {
        String newName = "New Group Name";

        groupEventService.groupRenamed(conversationId, newName, triggeredBy);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(GroupUpdatedEvent.class);

        verify(rabbitTemplate).convertAndSend(eq("group.exchange"), routingKeyCaptor.capture(), eventCaptor.capture());

        assertEquals("group.renamed", routingKeyCaptor.getValue());
        GroupUpdatedEvent event = eventCaptor.getValue();
        assertEquals(conversationId, event.getConversationId());
        assertEquals(newName, event.getNewName());
        assertEquals(triggeredBy, event.getTriggeredBy());
        assertEquals(GroupUpdatedEvent.UpdateType.RENAMED, event.getUpdateType());
    }
}
