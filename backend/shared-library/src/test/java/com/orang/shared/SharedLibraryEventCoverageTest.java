package com.orang.shared;

import com.orang.shared.event.ContactRequestSentEvent;
import com.orang.shared.event.DirectConversationCreatedEvent;
import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.GroupUpdatedEvent;
import com.orang.shared.event.MessageDeletedEvent;
import com.orang.shared.event.MessageEditedEvent;
import com.orang.shared.event.MessagePinnedEvent;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.event.MentionEvent;
import com.orang.shared.event.ThumbnailReadyEvent;
import com.orang.shared.event.UnreadCountEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SharedLibraryEventCoverageTest {

    @Test
    @DisplayName("message event builders populate inherited and local fields")
    void messageEventBuilders_PopulateInheritedAndLocalFields() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        UUID mentionedUserId = UUID.randomUUID();

        MessageEditedEvent editedEvent = MessageEditedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .newContent("edited")
                .editedAt(LocalDateTime.now())
                .build();

        MessageDeletedEvent deletedEvent = MessageDeletedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .deletedBy(deletedBy)
                .deletedAt(LocalDateTime.now())
                .build();

        MentionEvent mentionEvent = MentionEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .mentionedUserId(mentionedUserId)
                .contentPreview("hello @user")
                .build();

        assertThat(editedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(editedEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(editedEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(editedEvent.getNewContent()).isEqualTo("edited");
        assertThat(editedEvent.getEditedAt()).isNotNull();

        assertThat(deletedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(deletedEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(deletedEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(deletedEvent.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(deletedEvent.getDeletedAt()).isNotNull();

        assertThat(mentionEvent.getMessageId()).isEqualTo(messageId);
        assertThat(mentionEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(mentionEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(mentionEvent.getMentionedUserId()).isEqualTo(mentionedUserId);
        assertThat(mentionEvent.getContentPreview()).isEqualTo("hello @user");
    }

    @Test
    @DisplayName("reaction and pin event builders populate enums and counts")
    void reactionAndPinBuilders_PopulateEnumsAndCounts() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MessageReactionEvent reactionEvent = MessageReactionEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .action(MessageReactionEvent.Action.CHANGED)
                .reactionType("ORANG")
                .currentCounts(Map.of("LIKE", 5L, "ORANG", 2L))
                .messageAuthorId(authorId)
                .build();

        MessagePinnedEvent pinnedEvent = MessagePinnedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .action(MessagePinnedEvent.Action.PINNED)
                .build();

        assertThat(reactionEvent.getAction()).isEqualTo(MessageReactionEvent.Action.CHANGED);
        assertThat(reactionEvent.getReactionType()).isEqualTo("ORANG");
        assertThat(reactionEvent.getCurrentCounts()).containsEntry("LIKE", 5L).containsEntry("ORANG", 2L);
        assertThat(reactionEvent.getMessageAuthorId()).isEqualTo(authorId);

        assertThat(pinnedEvent.getAction()).isEqualTo(MessagePinnedEvent.Action.PINNED);
        assertThat(MessageReactionEvent.Action.values()).containsExactly(
                MessageReactionEvent.Action.ADDED,
                MessageReactionEvent.Action.REMOVED,
                MessageReactionEvent.Action.CHANGED
        );
        assertThat(MessagePinnedEvent.Action.values()).containsExactly(
                MessagePinnedEvent.Action.PINNED,
                MessagePinnedEvent.Action.UNPINNED
        );
    }

    @Test
    @DisplayName("group and lightweight event builders populate fields")
    void groupAndLightweightBuilders_PopulateFields() {
        UUID conversationId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        GroupMemberEvent groupMemberEvent = GroupMemberEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .triggeredBy(triggeredBy)
                .eventType(GroupMemberEvent.EventType.MEMBER_ADDED)
                .build();

        GroupUpdatedEvent groupUpdatedEvent = GroupUpdatedEvent.builder()
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .updateType(GroupUpdatedEvent.UpdateType.RENAMED)
                .newName("New name")
                .build();

        ContactRequestSentEvent contactRequestSentEvent = ContactRequestSentEvent.builder()
                .contactId(contactId)
                .requesterId(requesterId)
                .recipientId(recipientId)
                .build();

        DirectConversationCreatedEvent directConversationCreatedEvent = DirectConversationCreatedEvent.builder()
                .conversationId(conversationId)
                .initiatorId(requesterId)
                .recipientId(recipientId)
                .build();

        ThumbnailReadyEvent thumbnailReadyEvent = ThumbnailReadyEvent.builder()
                .attachmentId(attachmentId)
                .conversationId(conversationId)
                .messageId(messageId)
                .thumbnailUrl("https://cdn.example.com/thumb.jpg")
                .build();

        UnreadCountEvent unreadCountEvent = UnreadCountEvent.builder()
                .userId(userId)
                .unreadCount(7L)
                .build();

        assertThat(groupMemberEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(groupMemberEvent.getUserId()).isEqualTo(userId);
        assertThat(groupMemberEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(groupMemberEvent.getEventType()).isEqualTo(GroupMemberEvent.EventType.MEMBER_ADDED);
        assertThat(groupMemberEvent.getTimestamp()).isNotNull();

        assertThat(groupUpdatedEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(groupUpdatedEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(groupUpdatedEvent.getUpdateType()).isEqualTo(GroupUpdatedEvent.UpdateType.RENAMED);
        assertThat(groupUpdatedEvent.getNewName()).isEqualTo("New name");
        assertThat(groupUpdatedEvent.getTimestamp()).isNotNull();

        assertThat(contactRequestSentEvent.getContactId()).isEqualTo(contactId);
        assertThat(contactRequestSentEvent.getRequesterId()).isEqualTo(requesterId);
        assertThat(contactRequestSentEvent.getRecipientId()).isEqualTo(recipientId);

        assertThat(directConversationCreatedEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(directConversationCreatedEvent.getInitiatorId()).isEqualTo(requesterId);
        assertThat(directConversationCreatedEvent.getRecipientId()).isEqualTo(recipientId);
        assertThat(directConversationCreatedEvent.getTimestamp()).isNotNull();

        assertThat(thumbnailReadyEvent.getAttachmentId()).isEqualTo(attachmentId);
        assertThat(thumbnailReadyEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(thumbnailReadyEvent.getMessageId()).isEqualTo(messageId);
        assertThat(thumbnailReadyEvent.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.jpg");

        assertThat(unreadCountEvent.getUserId()).isEqualTo(userId);
        assertThat(unreadCountEvent.getUnreadCount()).isEqualTo(7L);
    }
}