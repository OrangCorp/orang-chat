package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public Page<MessageResponse> getMessagesForConversation(
            UUID conversationId,
            UUID requesterId,
            Pageable pageable) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(requesterId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(this::toMessageResponse);
    }

    @Transactional
    public void saveMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation not found with id: " + conversationId));

        if (!conversation.getParticipantIds().contains(senderId)) {
            throw new IllegalArgumentException(
                    "User " + senderId + " is not a participant in conversation " + conversationId);
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .build();

        messageRepository.save(message);
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
