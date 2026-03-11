package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public List<MessageResponse> getMessagesForConversation(UUID conversationId, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(requesterId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream().map(this::toMessageResponse).toList();
    }

    @Transactional
    public MessageResponse saveMessage(UUID conversationId, UUID senderId, String content) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .build();
        return toMessageResponse(messageRepository.save(message));
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
