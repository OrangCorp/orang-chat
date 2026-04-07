package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final MessageEventPublisher messageEventPublisher;
    private final AttachmentService attachmentService;

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
    public MessageResponse saveMessage(UUID conversationId,
                                       UUID senderId,
                                       String content,
                                       List<UUID> attachmentIds) {
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

        Message saved = messageRepository.save(message);

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentService.linkAttachmentsToMessage(attachmentIds, saved.getId(), senderId);
            // Refresh to load linked attachments
            saved = messageRepository.findById(saved.getId()).orElseThrow();
        }

        messageEventPublisher.publishMessageSent(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                saved.getContent(),
                attachmentIds
        );

        log.info("Message {} created by user {} with {} attachments",
                saved.getId(), senderId, attachmentIds != null ? attachmentIds.size() : 0);

        return messageMapper.toMessageResponse(saved);
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

        messageEventPublisher.publishMessageEdited(
                saved.getId(),
                saved.getConversationId(),
                userId,
                saved.getContent(),
                saved.getEditedAt()
        );

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

        messageEventPublisher.publishMessageDeleted(
                saved.getId(),
                saved.getConversationId(),
                saved.getDeletedBy(),
                saved.getDeletedAt()
        );
    }
}