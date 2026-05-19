package com.orang.messageservice.controller;

import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.service.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionControllerTest {

    @Mock
    private ReactionService reactionService;

    @InjectMocks
    private ReactionController reactionController;

    private UUID messageId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void toggleReactionDelegatesToService() {
        Map<ReactionType, Long> expected = Map.of(ReactionType.ORANG, 2L);
        when(reactionService.toggleReaction(messageId, userId, ReactionType.ORANG)).thenReturn(expected);

        var response = reactionController.toggleReaction(messageId, ReactionType.ORANG, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(reactionService).toggleReaction(messageId, userId, ReactionType.ORANG);
    }

    @Test
    void getReactionsDelegatesToService() {
        Map<ReactionType, Long> expected = Map.of(ReactionType.LIKE, 4L);
        when(reactionService.getReactionCounts(messageId)).thenReturn(expected);

        var response = reactionController.getReactions(messageId);

        assertThat(response.getBody()).isEqualTo(expected);
        verify(reactionService).getReactionCounts(messageId);
    }

    @Test
    void getMyReactionDelegatesToService() {
        Optional<ReactionType> expected = Optional.of(ReactionType.HEART);
        when(reactionService.getReactionType(messageId, userId)).thenReturn(expected);

        var response = reactionController.getMyReaction(messageId, userId.toString());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(reactionService).getReactionType(messageId, userId);
    }
}
