package com.orang.messageservice.service;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public List<ConversationResponse> getConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantIdsContaining(userId);
        return conversations.stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID userId1, UUID userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself");
        }

        Optional<Conversation> existingConversation =
                conversationRepository.findDirectConversationBetween(userId1, userId2);

        if (existingConversation.isPresent()) {
            return toConversationResponse(existingConversation.get());
        }

        Conversation newConversation = Conversation.builder()
                .type(Conversation.ConversationType.DIRECT)
                .participantIds(Set.of(userId1, userId2))
                .build();

        return toConversationResponse(conversationRepository.save(newConversation));
    }

    @Transactional
    public ConversationResponse createGroupConversation(String name, Set<UUID> participantIds, UUID creatorId) {
        Set<UUID> uniqueParticipantIds = new HashSet<>(participantIds); // Safely create a new modifiable set and add the creator
        uniqueParticipantIds.add(creatorId);

        if (uniqueParticipantIds.size() < 3) {
            throw new BadRequestException("A group conversation must have at least 3 participants");
        }

        Conversation newConversation = Conversation.builder()
                .type(Conversation.ConversationType.GROUP)
                .name(name)
                .participantIds(uniqueParticipantIds)
                .build();

        return toConversationResponse(conversationRepository.save(newConversation));
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .participantIds(conversation.getParticipantIds())
                .build();
    }

    public void verifyParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(userId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
    }
}
