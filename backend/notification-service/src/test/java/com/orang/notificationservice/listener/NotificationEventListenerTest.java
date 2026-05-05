package com.orang.notificationservice.listener;

import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.service.WebPushService;
import com.orang.shared.event.ContactRequestSentEvent;
import com.orang.shared.event.DirectConversationCreatedEvent;
import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.MentionEvent;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.event.MessageSentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private WebPushService webPushService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(webPushService);
    }

    @Test
    void handleContactRequestSentSendsNotificationToRecipient() {
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        listener.handleContactRequestSent(ContactRequestSentEvent.builder()
                .contactId(contactId)
                .requesterId(requesterId)
                .recipientId(recipientId)
                .build());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(webPushService).sendToUser(eq(recipientId), payloadCaptor.capture());

        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getTitle()).isEqualTo("New Contact Request");
        assertThat(payload.getBody()).isEqualTo("You have a new contact request");
        assertThat(payload.getTag()).isEqualTo("contact-request-" + contactId);
        assertThat(payload.getData().getType()).isEqualTo("contact_request");
        assertThat(payload.getData().getUrl()).isEqualTo("/contacts/pending/incoming");
    }

    @Test
    void handleDirectConversationCreatedSendsNotificationToRecipient() {
        UUID conversationId = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");
        UUID initiatorId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        listener.handleDirectConversationCreated(DirectConversationCreatedEvent.builder()
                .conversationId(conversationId)
                .initiatorId(initiatorId)
                .recipientId(recipientId)
                .build());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(webPushService).sendToUser(eq(recipientId), payloadCaptor.capture());

        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getTitle()).isEqualTo("New Chat");
        assertThat(payload.getBody()).isEqualTo("Someone started a conversation with you");
        assertThat(payload.getTag()).isEqualTo("direct-chat-created-" + conversationId);
        assertThat(payload.getData().getType()).isEqualTo("direct_chat_created");
        assertThat(payload.getData().getConversationId()).isEqualTo(conversationId);
        assertThat(payload.getData().getUrl()).isEqualTo("/conversations/" + conversationId);
    }

        @Test
        void handleContactRequestSentSkipsWhenRecipientMissing() {
        listener.handleContactRequestSent(ContactRequestSentEvent.builder()
            .contactId(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
            .requesterId(UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7"))
            .build());

        verifyNoInteractions(webPushService);
        }

        @Test
        void handleDirectConversationCreatedSkipsWhenDataMissing() {
        listener.handleDirectConversationCreated(DirectConversationCreatedEvent.builder()
            .conversationId(null)
            .recipientId(UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2"))
            .build());

        verifyNoInteractions(webPushService);
        }

        @Test
        void handleMessageSentTruncatesAndSendsToConversation() {
        UUID conversationId = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");
        UUID messageId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");
        UUID senderId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        Set<UUID> participants = Set.of(
            senderId,
            UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2")
        );

        listener.handleMessageSent(MessageSentEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(senderId)
            .participantIds(participants)
            .content("x".repeat(120))
            .build());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(webPushService).sendToConversation(eq(conversationId), eq(participants), eq(senderId), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getBody()).endsWith("...");
        }

        @Test
        void handleMessageSentSkipsWhenNoParticipants() {
        listener.handleMessageSent(MessageSentEvent.builder()
            .conversationId(UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd"))
            .messageId(UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111"))
            .triggeredBy(UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7"))
            .participantIds(Set.of())
            .content("Hello")
            .build());

        verifyNoInteractions(webPushService);
        }

        @Test
        void handleReactionSendsEmojiNotificationAndSkipsNonAddOrSelfReaction() {
        UUID conversationId = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");
        UUID messageId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");
        UUID authorId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID reactorId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");

        listener.handleReaction(MessageReactionEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(reactorId)
            .messageAuthorId(authorId)
            .action(MessageReactionEvent.Action.REMOVED)
            .reactionType("ORANG")
            .build());

        verifyNoInteractions(webPushService);

        listener.handleReaction(MessageReactionEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(authorId)
            .messageAuthorId(authorId)
            .action(MessageReactionEvent.Action.ADDED)
            .reactionType("ORANG")
            .build());

        verifyNoInteractions(webPushService);

        listener.handleReaction(MessageReactionEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(reactorId)
            .messageAuthorId(authorId)
            .action(MessageReactionEvent.Action.ADDED)
            .reactionType("ORANG")
            .build());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(webPushService).sendToUser(eq(authorId), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getBody()).contains("🍊");
        }

        @Test
        void handleMemberAddedAndMentionCoverRemainingBranches() {
        UUID conversationId = UUID.fromString("49b32d01-5a28-4013-b5a0-0651fe20adfd");
        UUID userId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
        UUID senderId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID messageId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

        listener.handleMemberAdded(GroupMemberEvent.builder()
            .conversationId(conversationId)
            .userId(userId)
            .triggeredBy(senderId)
            .eventType(GroupMemberEvent.EventType.MEMBER_REMOVED)
            .timestamp(LocalDateTime.of(2026, 5, 5, 9, 0))
            .build());
        verifyNoInteractions(webPushService);

        listener.handleMemberAdded(GroupMemberEvent.builder()
            .conversationId(conversationId)
            .userId(userId)
            .triggeredBy(senderId)
            .eventType(GroupMemberEvent.EventType.MEMBER_ADDED)
            .timestamp(LocalDateTime.of(2026, 5, 5, 9, 0))
            .build());
        verify(webPushService).sendToUser(eq(userId), org.mockito.ArgumentMatchers.any(NotificationPayload.class));

        org.mockito.Mockito.reset(webPushService);

        listener.handleMention(MentionEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(userId)
            .mentionedUserId(userId)
            .contentPreview("hello")
            .build());
        verify(webPushService, never()).sendToUser(eq(userId), org.mockito.ArgumentMatchers.any(NotificationPayload.class));

        listener.handleMention(MentionEvent.builder()
            .conversationId(conversationId)
            .messageId(messageId)
            .triggeredBy(senderId)
            .mentionedUserId(userId)
            .contentPreview("hello there")
            .build());
        verify(webPushService).sendToUser(eq(userId), org.mockito.ArgumentMatchers.any(NotificationPayload.class));
        }
}
