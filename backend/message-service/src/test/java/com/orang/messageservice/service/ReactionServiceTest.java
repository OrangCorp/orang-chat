package com.orang.messageservice.service;

import com.orang.messageservice.dto.ReactionCountProjection;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.MessageReaction;
import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.repository.MessageReactionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.event.MessageReactionEvent;
import com.orang.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @InjectMocks
    private ReactionService reactionService;

    private UUID messageId;
    private UUID userId;
    private UUID conversationId;
    private UUID messageAuthorId;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageAuthorId = UUID.randomUUID();
    }

    @Test
    void toggleReactionThrowsWhenMessageMissing() {
        when(messageRepository.findActiveById(messageId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reactionService.toggleReaction(messageId, userId, ReactionType.LIKE));
    }

    @Test
    void toggleReactionRemovesWhenSameReactionAlreadyExists() {
        Message message = baseMessage();
        MessageReaction existing = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .reactionType(ReactionType.LIKE)
                .build();

        when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(message));
        when(reactionRepository.findByMessageIdAndUserId(messageId, userId)).thenReturn(Optional.of(existing));
        when(reactionRepository.countByMessageIdGroupByType(messageId))
                .thenReturn(List.of(projection(ReactionType.LIKE, 1L)));

        Map<ReactionType, Long> result = reactionService.toggleReaction(messageId, userId, ReactionType.LIKE);

        assertEquals(1L, result.get(ReactionType.LIKE));
        verify(reactionRepository).delete(existing);
        verify(messageEventPublisher).publishReactionChanged(
                eq(messageId),
                eq(conversationId),
                eq(userId),
                eq(MessageReactionEvent.Action.REMOVED),
                eq(ReactionType.LIKE),
                eq(result),
                eq(messageAuthorId)
        );
    }

    @Test
    void toggleReactionChangesWhenDifferentReactionAlreadyExists() {
        Message message = baseMessage();
        MessageReaction existing = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .reactionType(ReactionType.SAD)
                .build();

        when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(message));
        when(reactionRepository.findByMessageIdAndUserId(messageId, userId)).thenReturn(Optional.of(existing));
        when(reactionRepository.countByMessageIdGroupByType(messageId))
                .thenReturn(List.of(projection(ReactionType.HEART, 2L)));

        Map<ReactionType, Long> result = reactionService.toggleReaction(messageId, userId, ReactionType.HEART);

        assertEquals(ReactionType.HEART, existing.getReactionType());
        assertEquals(2L, result.get(ReactionType.HEART));
        verify(reactionRepository).save(existing);
        verify(messageEventPublisher).publishReactionChanged(
                eq(messageId),
                eq(conversationId),
                eq(userId),
                eq(MessageReactionEvent.Action.CHANGED),
                eq(ReactionType.HEART),
                eq(result),
                eq(messageAuthorId)
        );
    }

    @Test
    void toggleReactionAddsWhenNoReactionExists() {
        Message message = baseMessage();

        when(messageRepository.findActiveById(messageId)).thenReturn(Optional.of(message));
        when(reactionRepository.findByMessageIdAndUserId(messageId, userId)).thenReturn(Optional.empty());
        when(reactionRepository.countByMessageIdGroupByType(messageId))
                .thenReturn(List.of(projection(ReactionType.ORANG, 1L)));

        Map<ReactionType, Long> result = reactionService.toggleReaction(messageId, userId, ReactionType.ORANG);

        assertEquals(1L, result.get(ReactionType.ORANG));
        verify(reactionRepository).save(org.mockito.ArgumentMatchers.any(MessageReaction.class));
        verify(messageEventPublisher).publishReactionChanged(
                eq(messageId),
                eq(conversationId),
                eq(userId),
                eq(MessageReactionEvent.Action.ADDED),
                eq(ReactionType.ORANG),
                eq(result),
                eq(messageAuthorId)
        );
    }

    @Test
    void getReactionCountsFiltersOutNullRows() {
        when(reactionRepository.countByMessageIdGroupByType(messageId))
                .thenReturn(List.of(
                        projection(ReactionType.LIKE, 3L),
                        projection(null, 2L),
                        projection(ReactionType.HEART, null)
                ));

        Map<ReactionType, Long> counts = reactionService.getReactionCounts(messageId);

        assertEquals(1, counts.size());
        assertEquals(3L, counts.get(ReactionType.LIKE));
    }

    @Test
    void getReactionTypeReturnsOptionalFromRepository() {
        when(reactionRepository.findByMessageIdAndUserId(messageId, userId))
                .thenReturn(Optional.of(MessageReaction.builder().reactionType(ReactionType.WOW).build()));

        Optional<ReactionType> type = reactionService.getReactionType(messageId, userId);

        assertEquals(ReactionType.WOW, type.orElseThrow());
    }

    private Message baseMessage() {
        return Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(messageAuthorId)
                .content("test")
                .build();
    }

    private ReactionCountProjection projection(ReactionType type, Long count) {
        return new ReactionCountProjection() {
            @Override
            public ReactionType getReactionType() {
                return type;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}