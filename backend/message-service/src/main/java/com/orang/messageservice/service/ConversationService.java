package com.orang.messageservice.service;

import com.orang.messageservice.dto.ConversationResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.repository.ConversationRepository;
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
        List<Conversation> user1Chats = conversationRepository.findByParticipantIdsContaining(userId1);

        Optional<Conversation> existingConversation = user1Chats.stream()
                .filter(c -> c.getType() == Conversation.ConversationType.DIRECT
                        && c.getParticipantIds().contains(userId2))
                .findFirst();

        if (existingConversation.isPresent()) {
            return toConversationResponse(existingConversation.get());
        }

        Conversation newConversation = Conversation.builder()
                .type(Conversation.ConversationType.DIRECT)
                .participantIds(Set.of(userId1, userId2))
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
}
