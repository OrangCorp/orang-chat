package com.orang.notificationservice.listener;

import com.orang.notificationservice.config.RabbitMQConfig;
import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.entity.NotificationType;
import com.orang.notificationservice.service.NotificationPersistenceService;
import com.orang.notificationservice.service.UnreadCountBroadcastService;
import com.orang.notificationservice.service.WebPushService;
import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final WebPushService webPushService;
    private final NotificationPersistenceService persistenceService;
    private final UnreadCountBroadcastService broadcastService;

    // =========================================================================
    // Contact Request
    // =========================================================================

    @RabbitListener(queues = RabbitMQConstants.CONTACT_REQUEST_NOTIFICATION_QUEUE)
    public void handleContactRequestSent(ContactRequestSentEvent event) {
        log.info("Received ContactRequestSentEvent: contactId={}, requesterId={}, recipientId={}",
                event.getContactId(), event.getRequesterId(), event.getRecipientId());

        if (event.getRecipientId() == null) {
            log.warn("ContactRequestSentEvent has no recipientId - skipping");
            return;
        }

        String title = "New Contact Request";
        String body = "You have a new contact request";

        try {
            // 1. Save — no groupKey, each contact request is a distinct notification
            persistenceService.saveNotification(
                    event.getRecipientId(),
                    NotificationType.CONTACT_REQUEST,
                    title,
                    body,
                    null,
                    null,
                    null,
                    event.getRequesterId()
            );

            // 2. Broadcast updated unread count via RabbitMQ → Chat Service → WebSocket
            broadcastService.broadcast(event.getRecipientId());

            // 3. Push
            webPushService.sendToUser(event.getRecipientId(), NotificationPayload.builder()
                    .title(title)
                    .body(body)
                    .icon("/icons/app-icon-192.png")
                    .tag("contact-request-" + event.getContactId())
                    .requireInteraction(true)
                    .data(NotificationPayload.NotificationData.builder()
                            .type("contact_request")
                            .url("/contacts/pending/incoming")
                            .build())
                    .build());

            log.info("Processed contact request notification for recipient {}", event.getRecipientId());

        } catch (Exception e) {
            log.error("Failed to process ContactRequestSentEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Direct Conversation Created
    // =========================================================================

    @RabbitListener(queues = RabbitMQConfig.DIRECT_CONVERSATION_CREATED_NOTIFICATION_QUEUE)
    public void handleDirectConversationCreated(DirectConversationCreatedEvent event) {
        log.info("Received DirectConversationCreatedEvent: conversationId={}, initiatorId={}, recipientId={}",
                event.getConversationId(), event.getInitiatorId(), event.getRecipientId());

        if (event.getRecipientId() == null || event.getConversationId() == null) {
            log.warn("DirectConversationCreatedEvent missing fields - skipping");
            return;
        }

        String title = "New Chat";
        String body = "Someone started a conversation with you";

        try {
            // 1. Save
            persistenceService.saveNotification(
                    event.getRecipientId(),
                    NotificationType.DIRECT_CONVERSATION_CREATED,
                    title,
                    body,
                    null,
                    event.getConversationId(),
                    null,
                    event.getInitiatorId()
            );

            // 2. Broadcast
            broadcastService.broadcast(event.getRecipientId());

            // 3. Push
            webPushService.sendToUser(event.getRecipientId(), NotificationPayload.builder()
                    .title(title)
                    .body(body)
                    .icon("/icons/app-icon-192.png")
                    .tag("direct-chat-created-" + event.getConversationId())
                    .requireInteraction(true)
                    .data(NotificationPayload.NotificationData.builder()
                            .type("direct_chat_created")
                            .conversationId(event.getConversationId())
                            .url("/conversations/" + event.getConversationId())
                            .build())
                    .build());

            log.info("Processed direct conversation notification for recipient {}", event.getRecipientId());

        } catch (Exception e) {
            log.error("Failed to process DirectConversationCreatedEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // New Message — grouped per conversation
    // =========================================================================

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_NOTIFICATION_QUEUE)
    public void handleMessageSent(MessageSentEvent event) {
        log.info("Received MessageSentEvent: conversationId={}, messageId={}, sender={}, participants={}",
                event.getConversationId(), event.getMessageId(), event.getTriggeredBy(),
                event.getParticipantIds() != null ? event.getParticipantIds().size() : 0);

        if (event.getParticipantIds() == null || event.getParticipantIds().isEmpty()) {
            log.warn("MessageSentEvent has no participantIds - skipping");
            return;
        }

        // All messages in the same conversation share this group key.
        // If an unread notification already exists for this key, we increment
        // its count instead of creating a duplicate row.
        String groupKey = "conv:" + event.getConversationId() + ":messages";
        String title = "New Message";
        String body = truncateContent(event.getContent());

        // Save + broadcast per recipient (each user has their own notification row)
        for (UUID recipientId : event.getParticipantIds()) {
            if (recipientId.equals(event.getTriggeredBy())) {
                continue; // never notify the sender
            }

            try {
                // 1. Save (upsert — increments count if unread group exists)
                persistenceService.saveNotification(
                        recipientId,
                        NotificationType.NEW_MESSAGE,
                        title,
                        body,
                        groupKey,
                        event.getConversationId(),
                        event.getMessageId(),
                        event.getTriggeredBy()
                );

                // 2. Broadcast
                broadcastService.broadcast(recipientId);

            } catch (Exception e) {
                // Log and continue — one failed recipient should not block others
                log.error("Failed to save/broadcast notification for recipient {}: {}",
                        recipientId, e.getMessage(), e);
            }
        }

        // 3. Push — existing bulk send handles per-user mute checks internally
        try {
            webPushService.sendToConversation(
                    event.getConversationId(),
                    event.getParticipantIds(),
                    event.getTriggeredBy(),
                    NotificationPayload.builder()
                            .title(title)
                            .body(body)
                            .icon("/icons/app-icon-192.png")
                            .tag("conversation-" + event.getConversationId())
                            .data(NotificationPayload.NotificationData.builder()
                                    .type("new_message")
                                    .conversationId(event.getConversationId())
                                    .messageId(event.getMessageId())
                                    .url("/conversations/" + event.getConversationId())
                                    .build())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to send push for MessageSentEvent conv={}: {}",
                    event.getConversationId(), e.getMessage(), e);
        }

        log.info("Processed message notification for conversation {} ({} recipients)",
                event.getConversationId(), event.getParticipantIds().size() - 1);
    }

    // =========================================================================
    // Reaction — grouped per message
    // =========================================================================

    @RabbitListener(queues = RabbitMQConfig.REACTION_NOTIFICATION_QUEUE)
    public void handleReaction(MessageReactionEvent event) {
        log.info("Received MessageReactionEvent: messageId={}, action={}, type={}, author={}",
                event.getMessageId(), event.getAction(), event.getReactionType(),
                event.getMessageAuthorId());

        if (event.getAction() != MessageReactionEvent.Action.ADDED) {
            log.debug("Skipping notification for reaction action: {}", event.getAction());
            return;
        }

        if (event.getMessageAuthorId() == null) {
            log.warn("MessageReactionEvent has no messageAuthorId - skipping");
            return;
        }

        if (event.getMessageAuthorId().equals(event.getTriggeredBy())) {
            log.debug("Skipping notification - user reacted to their own message");
            return;
        }

        String emoji = getEmojiForReaction(event.getReactionType());
        String title = "New Reaction";
        String body = "Someone reacted " + emoji + " to your message";

        // Reactions on the same message are grouped together.
        // Multiple people reacting = one notification with count.
        String groupKey = "msg:" + event.getMessageId() + ":reactions";

        try {
            // 1. Save
            persistenceService.saveNotification(
                    event.getMessageAuthorId(),
                    NotificationType.REACTION,
                    title,
                    body,
                    groupKey,
                    event.getConversationId(),
                    event.getMessageId(),
                    event.getTriggeredBy()
            );

            // 2. Broadcast
            broadcastService.broadcast(event.getMessageAuthorId());

            // 3. Push
            webPushService.sendToUser(event.getMessageAuthorId(), NotificationPayload.builder()
                    .title(title)
                    .body(body)
                    .icon("/icons/app-icon-192.png")
                    .tag("reaction-" + event.getMessageId())
                    .data(NotificationPayload.NotificationData.builder()
                            .type("reaction")
                            .conversationId(event.getConversationId())
                            .messageId(event.getMessageId())
                            .url("/conversations/" + event.getConversationId()
                                    + "?highlight=" + event.getMessageId())
                            .build())
                    .build());

            log.info("Processed reaction notification for user {}", event.getMessageAuthorId());

        } catch (Exception e) {
            log.error("Failed to process MessageReactionEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Mention — never grouped, always individual
    // =========================================================================

    @RabbitListener(queues = RabbitMQConfig.MENTION_NOTIFICATION_QUEUE)
    public void handleMention(MentionEvent event) {
        log.info("Received MentionEvent: messageId={}, mentionedUser={}, sender={}",
                event.getMessageId(), event.getMentionedUserId(), event.getTriggeredBy());

        if (event.getMentionedUserId().equals(event.getTriggeredBy())) {
            log.debug("Skipping notification - user mentioned themselves");
            return;
        }

        String title = "You were mentioned";
        String body = truncateContent(event.getContentPreview());

        try {
            // 1. Save — null groupKey means no grouping, every mention is its own row
            persistenceService.saveNotification(
                    event.getMentionedUserId(),
                    NotificationType.MENTION,
                    title,
                    body,
                    null,
                    event.getConversationId(),
                    event.getMessageId(),
                    event.getTriggeredBy()
            );

            // 2. Broadcast
            broadcastService.broadcast(event.getMentionedUserId());

            // 3. Push — mentions bypass mute (you always want to know you were mentioned)
            webPushService.sendToUser(event.getMentionedUserId(), NotificationPayload.builder()
                    .title(title)
                    .body(body)
                    .icon("/icons/app-icon-192.png")
                    .tag("mention-" + event.getConversationId())
                    .requireInteraction(true)
                    .data(NotificationPayload.NotificationData.builder()
                            .type("mention")
                            .conversationId(event.getConversationId())
                            .messageId(event.getMessageId())
                            .url("/conversations/" + event.getConversationId()
                                    + "?highlight=" + event.getMessageId())
                            .build())
                    .build());

            log.info("Processed mention notification for user {}", event.getMentionedUserId());

        } catch (Exception e) {
            log.error("Failed to process MentionEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Group Member Added
    // =========================================================================

    @RabbitListener(queues = RabbitMQConfig.MEMBER_ADDED_NOTIFICATION_QUEUE)
    public void handleMemberAdded(GroupMemberEvent event) {
        log.info("Received GroupMemberEvent: conversationId={}, userId={}, type={}",
                event.getConversationId(), event.getUserId(), event.getEventType());

        if (event.getEventType() != GroupMemberEvent.EventType.MEMBER_ADDED) {
            log.debug("Skipping notification for event type: {}", event.getEventType());
            return;
        }

        String title = "Added to Group";
        String body = "You were added to a new group conversation";

        try {
            // 1. Save — not grouped, being added to a group is a distinct actionable event
            persistenceService.saveNotification(
                    event.getUserId(),
                    NotificationType.GROUP_ADDED,
                    title,
                    body,
                    null,
                    event.getConversationId(),
                    null,
                    event.getTriggeredBy()
            );

            // 2. Broadcast
            broadcastService.broadcast(event.getUserId());

            // 3. Push
            webPushService.sendToUser(event.getUserId(), NotificationPayload.builder()
                    .title(title)
                    .body(body)
                    .icon("/icons/app-icon-192.png")
                    .tag("group-added-" + event.getConversationId())
                    .requireInteraction(true)
                    .data(NotificationPayload.NotificationData.builder()
                            .type("group_added")
                            .conversationId(event.getConversationId())
                            .url("/conversations/" + event.getConversationId())
                            .build())
                    .build());

            log.info("Processed group added notification for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process GroupMemberEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String truncateContent(String content) {
        if (content == null || content.isBlank()) return "Sent a message";
        if (content.length() <= 100) return content;
        return content.substring(0, 97) + "...";
    }

    private String getEmojiForReaction(String reactionType) {
        if (reactionType == null) return "?";
        return switch (reactionType.toUpperCase()) {
            case "LIKE" -> "👍";
            case "HEART", "LOVE" -> "❤️";
            case "LAUGH", "HAHA" -> "😂";
            case "WOW", "SURPRISED" -> "😮";
            case "SAD" -> "😢";
            case "ANGRY" -> "😡";
            default -> "👍";
        };
    }
}