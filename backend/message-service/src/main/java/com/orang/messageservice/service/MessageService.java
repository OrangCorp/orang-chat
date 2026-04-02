package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.event.MessageDeletedInternalEvent;
import com.orang.messageservice.event.MessageEditedInternalEvent;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

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
        return messages.map(messageMapper::toMessageResponse);
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

    @Transactional
    public MessageResponse editMessage(UUID messageId, UUID userId, String newContent) {
        Message message = messageRepository.findActiveById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new ForbiddenException("Only the sender can edit this message");
        }

        message.setContent(newContent);
        message.markAsEdited();

        Message saved = messageRepository.save(message);
        log.info("Message {} edited by user {}", messageId, userId);

        applicationEventPublisher.publishEvent(new MessageEditedInternalEvent(
                saved.getId(),
                saved.getConversationId(),
                userId,
                saved.getContent(),
                saved.getEditedAt()
        ));

        return messageMapper.toMessageResponse(saved);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findActiveById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        boolean isSender = message.getSenderId().equals(userId);
        boolean isAdmin = conversationService.isAdmin(message.getConversationId(), userId);

        if (!isSender && !isAdmin) {
            throw new ForbiddenException("You cannot delete this message");
        }

        message.softDelete(userId);
        Message saved = messageRepository.save(message);

        log.info("Message {} deleted by user {} (isSender: {}, isAdmin: {})",
                messageId, userId, isSender, isAdmin);

        applicationEventPublisher.publishEvent(new MessageDeletedInternalEvent(
                saved.getId(),
                saved.getConversationId(),
                saved.getDeletedBy(),
                saved.getDeletedAt()
        ));
    }
}