package com.orang.notificationservice.listener;

import com.orang.notificationservice.config.RabbitMQConfig;
import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.service.WebPushService;
import com.orang.shared.event.GroupMemberEvent;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens to RabbitMQ events and triggers push notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final WebPushService webPushService;

    // =========================================================================
    // Message Notifications
    // =========================================================================

    /**
     * Handle new message event.
     *
     * Notifies all conversation participants except the sender.
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_NOTIFICATION_QUEUE)
    public void handleMessageSent(MessageSentEvent event) {
        log.info("Received MessageSentEvent: conversationId={}, messageId={}, sender={}, participants={}",
                event.getConversationId(),
                event.getMessageId(),
                event.getTriggeredBy(),
                event.getParticipantIds() != null ? event.getParticipantIds().size() : 0);

        // Validate we have participants
        if (event.getParticipantIds() == null || event.getParticipantIds().isEmpty()) {
            log.warn("MessageSentEvent has no participantIds - skipping notification");
            return;
        }

        try {
            NotificationPayload payload = NotificationPayload.builder()
                    .title("New Message")
                    .body(truncateContent(event.getContent()))
                    .icon("/icons/app-icon-192.png")
                    .tag("conversation-" + event.getConversationId())
                    .data(NotificationPayload.NotificationData.builder()
                            .type("new_message")
                            .conversationId(event.getConversationId())
                            .messageId(event.getMessageId())
                            .url("/conversations/" + event.getConversationId())
                            .build())
                    .build();

            // Send to all participants except sender
            webPushService.sendToConversation(
                    event.getConversationId(),
                    event.getParticipantIds(),
                    event.getTriggeredBy(),  // Sender - will be excluded
                    payload
            );

            log.info("Processed new message notification for conversation {} ({} potential recipients)",
                    event.getConversationId(),
                    event.getParticipantIds().size() - 1);  // Minus sender

        } catch (Exception e) {
            log.error("Failed to process MessageSentEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Reaction Notifications
    // =========================================================================

    /**
     * Handle reaction event.
     *
     * Notifies the message author when someone reacts to their message.
     * Does not notify if:
     * - Action is not ADDED (removed/changed reactions don't notify)
     * - User reacted to their own message
     */
    @RabbitListener(queues = RabbitMQConfig.REACTION_NOTIFICATION_QUEUE)
    public void handleReaction(MessageReactionEvent event) {
        log.info("Received MessageReactionEvent: messageId={}, action={}, type={}, author={}",
                event.getMessageId(),
                event.getAction(),
                event.getReactionType(),
                event.getMessageAuthorId());

        // Only notify on reaction added
        if (event.getAction() != MessageReactionEvent.Action.ADDED) {
            log.debug("Skipping notification for reaction action: {}", event.getAction());
            return;
        }

        // Validate we have message author
        if (event.getMessageAuthorId() == null) {
            log.warn("MessageReactionEvent has no messageAuthorId - skipping notification");
            return;
        }

        // Don't notify if user reacted to their own message
        if (event.getMessageAuthorId().equals(event.getTriggeredBy())) {
            log.debug("Skipping notification - user reacted to their own message");
            return;
        }

        try {
            String emoji = getEmojiForReaction(event.getReactionType());

            NotificationPayload payload = NotificationPayload.builder()
                    .title("New Reaction")
                    .body("Someone reacted " + emoji + " to your message")
                    .icon("/icons/app-icon-192.png")
                    .tag("reaction-" + event.getMessageId())
                    .data(NotificationPayload.NotificationData.builder()
                            .type("reaction")
                            .conversationId(event.getConversationId())
                            .messageId(event.getMessageId())
                            .url("/conversations/" + event.getConversationId() +
                                    "?highlight=" + event.getMessageId())
                            .build())
                    .build();

            // Send notification to message author
            webPushService.sendToUser(event.getMessageAuthorId(), payload);

            log.info("Sent reaction notification to user {} for message {}",
                    event.getMessageAuthorId(), event.getMessageId());

        } catch (Exception e) {
            log.error("Failed to process MessageReactionEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Group Member Notifications
    // =========================================================================

    /**
     * Handle group member event.
     *
     * Notifies user when they are added to a group.
     */
    @RabbitListener(queues = RabbitMQConfig.MEMBER_ADDED_NOTIFICATION_QUEUE)
    public void handleMemberAdded(GroupMemberEvent event) {
        log.info("Received GroupMemberEvent: conversationId={}, userId={}, type={}",
                event.getConversationId(), event.getUserId(), event.getEventType());

        // Only notify on member added
        if (event.getEventType() != GroupMemberEvent.EventType.MEMBER_ADDED) {
            log.debug("Skipping notification for event type: {}", event.getEventType());
            return;
        }

        try {
            NotificationPayload payload = NotificationPayload.builder()
                    .title("Added to Group")
                    .body("You were added to a new group conversation")
                    .icon("/icons/app-icon-192.png")
                    .tag("group-added-" + event.getConversationId())
                    .requireInteraction(true)
                    .data(NotificationPayload.NotificationData.builder()
                            .type("group_added")
                            .conversationId(event.getConversationId())
                            .url("/conversations/" + event.getConversationId())
                            .build())
                    .build();

            // Notify the user who was added
            webPushService.sendToUser(event.getUserId(), payload);

            log.info("Sent group added notification to user {} for conversation {}",
                    event.getUserId(), event.getConversationId());

        } catch (Exception e) {
            log.error("Failed to process GroupMemberEvent: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String truncateContent(String content) {
        if (content == null || content.isBlank()) {
            return "Sent a message";
        }
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 97) + "...";
    }

    private String getEmojiForReaction(String reactionType) {
        if (reactionType == null) {
            return "👍";
        }

        return switch (reactionType.toUpperCase()) {
            case "LIKE" -> "👍";
            case "HEART", "LOVE" -> "❤️";
            case "LAUGH", "HAHA" -> "😂";
            case "WOW", "SURPRISED" -> "😮";
            case "SAD" -> "😢";
            case "ANGRY" -> "😠";
            case "ORANG" -> "🍊";
            default -> "👍";
        };
    }
}