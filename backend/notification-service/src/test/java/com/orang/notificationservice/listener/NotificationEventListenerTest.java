package com.orang.notificationservice.listener;

import com.orang.notificationservice.service.NotificationPersistenceService;
import com.orang.notificationservice.service.UnreadCountBroadcastService;
import com.orang.notificationservice.service.WebPushService;
import com.orang.shared.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private WebPushService webPushService;

    @Mock
    private NotificationPersistenceService persistenceService;

    @Mock
    private UnreadCountBroadcastService broadcastService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(
                webPushService,
                persistenceService,
                broadcastService
        );
    }

    // =========================================================================
    // Contact Request
    // =========================================================================

    @Test
    void handleContactRequestSent_savesAndBroadcastsAndPushes() {
        UUID recipientId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        ContactRequestSentEvent event = new ContactRequestSentEvent();
        event.setRecipientId(recipientId);
        event.setRequesterId(requesterId);
        event.setContactId(contactId);

        listener.handleContactRequestSent(event);

        verify(persistenceService).saveNotification(
                eq(recipientId), any(), any(), any(), eq(null), eq(null), eq(null), eq(requesterId)
        );
        verify(broadcastService).broadcast(recipientId);
        verify(webPushService).sendToUser(eq(recipientId), any());
    }

    @Test
    void handleContactRequestSent_skips_whenRecipientNull() {
        ContactRequestSentEvent event = new ContactRequestSentEvent();
        event.setRecipientId(null);

        listener.handleContactRequestSent(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }

    // =========================================================================
    // Message Sent
    // =========================================================================

    @Test
    void handleMessageSent_savesForEachRecipientExceptSender() {
        UUID senderId = UUID.randomUUID();
        UUID recipient1 = UUID.randomUUID();
        UUID recipient2 = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MessageSentEvent event = new MessageSentEvent();
        event.setConversationId(conversationId);
        event.setMessageId(messageId);
        event.setTriggeredBy(senderId);
        event.setContent("Hello world");
        event.setParticipantIds(Set.of(senderId, recipient1, recipient2));

        listener.handleMessageSent(event);

        verify(persistenceService, times(2)).saveNotification(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(persistenceService, never()).saveNotification(
                eq(senderId), any(), any(), any(), any(), any(), any(), any()
        );
        verify(broadcastService).broadcast(recipient1);
        verify(broadcastService).broadcast(recipient2);
        verify(broadcastService, never()).broadcast(senderId);
    }

    @Test
    void handleMessageSent_skips_whenNoParticipants() {
        MessageSentEvent event = new MessageSentEvent();
        event.setParticipantIds(null);

        listener.handleMessageSent(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }

    @Test
    void handleMessageSent_usesGroupKey() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        MessageSentEvent event = new MessageSentEvent();
        event.setConversationId(conversationId);
        event.setMessageId(UUID.randomUUID());
        event.setTriggeredBy(senderId);
        event.setContent("Hello");
        event.setParticipantIds(Set.of(senderId, recipientId));

        listener.handleMessageSent(event);

        verify(persistenceService).saveNotification(
                eq(recipientId),
                any(),
                any(),
                any(),
                eq("conv:" + conversationId + ":messages"),
                eq(conversationId),
                any(),
                eq(senderId)
        );
    }

    // =========================================================================
    // Reaction
    // =========================================================================

    @Test
    void handleReaction_savesAndBroadcasts_whenAdded() {
        UUID authorId = UUID.randomUUID();
        UUID reactorId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        MessageReactionEvent event = new MessageReactionEvent();
        event.setMessageId(messageId);
        event.setConversationId(conversationId);
        event.setMessageAuthorId(authorId);
        event.setTriggeredBy(reactorId);
        event.setAction(MessageReactionEvent.Action.ADDED);
        event.setReactionType("LIKE");

        listener.handleReaction(event);

        verify(persistenceService).saveNotification(
                eq(authorId),
                any(),
                any(),
                any(),
                eq("msg:" + messageId + ":reactions"),
                eq(conversationId),
                eq(messageId),
                eq(reactorId)
        );
        verify(broadcastService).broadcast(authorId);
        verify(webPushService).sendToUser(eq(authorId), any());
    }

    @Test
    void handleReaction_skips_whenActionIsNotAdded() {
        MessageReactionEvent event = new MessageReactionEvent();
        event.setAction(MessageReactionEvent.Action.REMOVED);
        event.setMessageAuthorId(UUID.randomUUID());
        event.setTriggeredBy(UUID.randomUUID());

        listener.handleReaction(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }

    @Test
    void handleReaction_skips_whenUserReactsToOwnMessage() {
        UUID userId = UUID.randomUUID();

        MessageReactionEvent event = new MessageReactionEvent();
        event.setAction(MessageReactionEvent.Action.ADDED);
        event.setMessageAuthorId(userId);
        event.setTriggeredBy(userId);
        event.setReactionType("LIKE");

        listener.handleReaction(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }

    // =========================================================================
    // Mention
    // =========================================================================

    @Test
    void handleMention_savesWithNullGroupKey() {
        UUID mentionedUserId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MentionEvent event = new MentionEvent();
        event.setMentionedUserId(mentionedUserId);
        event.setTriggeredBy(senderId);
        event.setConversationId(conversationId);
        event.setMessageId(messageId);
        event.setContentPreview("Hey @user check this");

        listener.handleMention(event);

        verify(persistenceService).saveNotification(
                eq(mentionedUserId),
                any(),
                any(),
                any(),
                eq(null),
                eq(conversationId),
                eq(messageId),
                eq(senderId)
        );
        verify(broadcastService).broadcast(mentionedUserId);
        verify(webPushService).sendToUser(eq(mentionedUserId), any());
    }

    @Test
    void handleMention_skips_whenUserMentionsThemselves() {
        UUID userId = UUID.randomUUID();

        MentionEvent event = new MentionEvent();
        event.setMentionedUserId(userId);
        event.setTriggeredBy(userId);

        listener.handleMention(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }

    // =========================================================================
    // Group Member Added
    // =========================================================================

    @Test
    void handleMemberAdded_savesAndBroadcasts() {
        UUID userId = UUID.randomUUID();
        UUID addedById = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        GroupMemberEvent event = new GroupMemberEvent();
        event.setUserId(userId);
        event.setTriggeredBy(addedById);
        event.setConversationId(conversationId);
        event.setEventType(GroupMemberEvent.EventType.MEMBER_ADDED);

        listener.handleMemberAdded(event);

        verify(persistenceService).saveNotification(
                eq(userId),
                any(),
                any(),
                any(),
                eq(null),
                eq(conversationId),
                eq(null),
                eq(addedById)
        );
        verify(broadcastService).broadcast(userId);
        verify(webPushService).sendToUser(eq(userId), any());
    }

    @Test
    void handleMemberAdded_skips_whenEventTypeIsNotMemberAdded() {
        GroupMemberEvent event = new GroupMemberEvent();
        event.setEventType(GroupMemberEvent.EventType.MEMBER_REMOVED);
        event.setUserId(UUID.randomUUID());

        listener.handleMemberAdded(event);

        verifyNoInteractions(persistenceService, broadcastService, webPushService);
    }
}