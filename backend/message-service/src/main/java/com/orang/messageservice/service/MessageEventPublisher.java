package com.orang.messageservice.service;

import com.orang.messageservice.config.RabbitMQConfig;
import com.orang.messageservice.entity.ReactionType;
import com.orang.shared.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = RabbitMQConfig.CHAT_EXCHANGE;

    public void publishMessageEdited(UUID messageId, UUID conversationId, UUID userId,
                                     String newContent, LocalDateTime editedAt) {
        MessageEditedEvent event = MessageEditedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(userId)
                .timestamp(LocalDateTime.now())
                .newContent(newContent)
                .editedAt(editedAt)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "message.edited", event);
        log.info("Published MessageEditedEvent for message {}", messageId);
    }

    public void publishMessageDeleted(UUID messageId, UUID conversationId, UUID deletedBy,
                                      LocalDateTime deletedAt) {
        MessageDeletedEvent event = MessageDeletedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(deletedBy)
                .timestamp(LocalDateTime.now())
                .deletedBy(deletedBy)
                .deletedAt(deletedAt)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "message.deleted", event);
        log.info("Published MessageDeletedEvent for message {}", messageId);
    }

    public void publishReactionChanged(UUID messageId, UUID conversationId, UUID userId,
                                       MessageReactionEvent.Action action, ReactionType reactionType,
                                       Map<ReactionType, Long> currentCounts,
                                       UUID messageAuthorId) {

        Map<String, Long> countsAsString = currentCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        MessageReactionEvent event = MessageReactionEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(userId)
                .timestamp(LocalDateTime.now())
                .action(action)
                .reactionType(reactionType.name())
                .currentCounts(countsAsString)
                .messageAuthorId(messageAuthorId)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "message.reaction", event);
        log.info("Published MessageReactionEvent: {} {} on message {} (author: {})",
                action, reactionType, messageId, messageAuthorId);
    }

    public void publishMessagePinChanged(UUID messageId, UUID conversationId, UUID userId,
                                         MessagePinnedEvent.Action action) {
        MessagePinnedEvent event = MessagePinnedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(userId)
                .timestamp(LocalDateTime.now())
                .action(action)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "message.pin", event);
        log.info("Published MessagePinnedEvent: {} for message {}", action, messageId);
    }

    public void publishMessageSent(UUID messageId, UUID conversationId, UUID senderId,
                                   String content, List<UUID> attachmentIds,
                                   Set<UUID> participantIds) {
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(senderId)
                .timestamp(LocalDateTime.now())
                .content(content)
                .attachmentIds(attachmentIds != null ? attachmentIds : List.of())
                .participantIds(participantIds != null ? participantIds : Set.of())
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "message.sent", event);
        log.info("Published MessageSentEvent for message {} with {} attachments to {} participants",
                messageId,
                attachmentIds != null ? attachmentIds.size() : 0,
                participantIds != null ? participantIds.size() : 0);
    }
}