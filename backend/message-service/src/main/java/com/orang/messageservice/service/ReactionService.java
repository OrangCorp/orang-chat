package com.orang.messageservice.service;

import com.orang.messageservice.dto.ReactionCountProjection;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.MessageReaction;
import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.repository.MessageReactionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionService {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageEventPublisher messageEventPublisher;

    @Transactional
    public Map<ReactionType, Long> toggleReaction(UUID messageId, UUID userId, ReactionType reactionType) {
        Message message = messageRepository.findActiveById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        conversationService.verifyParticipant(message.getConversationId(), userId);

        Optional<MessageReaction> existingReaction = reactionRepository.findByMessageIdAndUserId(messageId, userId);

        MessageReactionEvent.Action action;

        if (existingReaction.isPresent()) {
            MessageReaction currentReaction = existingReaction.get();

            if (currentReaction.getReactionType() == reactionType) {
                reactionRepository.delete(currentReaction);
                log.info("Reaction {} removed from message {} by user {}", reactionType, messageId, userId);
                action = MessageReactionEvent.Action.REMOVED;
            } else {
                currentReaction.setReactionType(reactionType);
                reactionRepository.save(currentReaction);
                log.info("Reaction changed to {} on message {} by user {}", reactionType, messageId, userId);
                action = MessageReactionEvent.Action.CHANGED;
            }
        } else {
            MessageReaction newReaction = MessageReaction.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .reactionType(reactionType)
                    .build();

            reactionRepository.save(newReaction);
            log.info("Reaction {} added to message {} by user {}", reactionType, messageId, userId);
            action = MessageReactionEvent.Action.ADDED;
        }

        Map<ReactionType, Long> counts = getReactionCounts(messageId);

        messageEventPublisher.publishReactionChanged(
                messageId,
                message.getConversationId(),
                userId,
                action,
                reactionType,
                counts
        );

        return counts;
    }

    public Map<ReactionType, Long> getReactionCounts(UUID messageId) {
        List<ReactionCountProjection> counts = reactionRepository.countByMessageIdGroupByType(messageId);
        return counts.stream()
                .filter(count -> count.getReactionType() != null && count.getCount() != null)
                .collect(Collectors.toMap(
                        ReactionCountProjection::getReactionType,
                        ReactionCountProjection::getCount
                ));
    }

    public Optional<ReactionType> getReactionType(UUID messageId, UUID userId) {
        return reactionRepository.findByMessageIdAndUserId(messageId, userId)
                .map(MessageReaction::getReactionType);
    }
}