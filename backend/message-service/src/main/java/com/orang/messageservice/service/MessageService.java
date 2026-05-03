package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.MessageMention;
import com.orang.messageservice.mapper.MessageMapper;
import com.orang.messageservice.repository.ConversationRepository;
import com.orang.messageservice.repository.MessageMentionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final MentionParserService mentionParserService;
    private final MessageMentionRepository messageMentionRepository;

    @Transactional(readOnly = true)
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
        return messages.map(m -> messageMapper.toMessageResponse(m, requesterId));
    }

    @Transactional
    public MessageResponse saveMessage(UUID conversationId,
                                       UUID senderId,
                                       String content,
                                       List<UUID> attachmentIds,
                                       UUID replyToMessageId,
                                       UUID messageId) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation not found with id: " + conversationId));

        if (!conversation.getParticipantIds().contains(senderId)) {
            throw new IllegalArgumentException(
                    "User " + senderId + " is not a participant in conversation " + conversationId);
        }

        // If a client-generated messageId was provided and the message already exists,
        // return the existing message (handles duplicate/retry scenarios)
        if (messageId != null && messageRepository.existsById(messageId)) {
            log.info("Message {} already exists, returning existing", messageId);
            Message existing = messageRepository.findById(messageId).orElseThrow();
                        return messageMapper.toMessageResponse(existing, senderId);
        }

        // Validate reply reference
        UUID validatedReplyToId = null;
        if (replyToMessageId != null) {
            boolean replyExists = messageRepository.existsByIdAndConversationId(
                    replyToMessageId, conversationId);
            if (replyExists) {
                validatedReplyToId = replyToMessageId;
            } else {
                log.warn("Reply references invalid message {} in conversation {}, ignoring",
                        replyToMessageId, conversationId);
            }
        }

        // Build the message - let the database generate the ID
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .replyToMessageId(validatedReplyToId)
                .build();

        // Save without setting ID - database generates a new UUID
        Message saved = messageRepository.saveAndFlush(message);
        //log.info("Message persisted with ID: {}", saved.getId()); // ADD THIS

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentService.linkAttachmentsToMessage(attachmentIds, saved.getId(), senderId);
            saved = messageRepository.findById(saved.getId()).orElseThrow();
        }

        // Process mentions
        Set<UUID> participantIds = new HashSet<>(conversation.getParticipantIds());
        processMentions(saved, participantIds);

        messageEventPublisher.publishMessageSent(
                saved.getId(),
                saved.getConversationId(),
                saved.getSenderId(),
                saved.getContent(),
                attachmentIds,
                participantIds
        );

        log.info("Message {} created by user {} with {} attachments, replyTo={}",
                saved.getId(), senderId,
                attachmentIds != null ? attachmentIds.size() : 0,
                validatedReplyToId);

        return messageMapper.toMessageResponse(saved, senderId);
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

        return messageMapper.toMessageResponse(saved, userId);
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

    private void processMentions(Message message, Set<UUID> participantIds) {
        Set<UUID> extracted = mentionParserService.extractMentions(message.getContent());
        if (extracted.isEmpty()) {
            return;
        }

        Set<UUID> valid = mentionParserService.validateMentions(extracted, participantIds);
        if (valid.isEmpty()) {
            return;
        }

        List<MessageMention> mentions = valid.stream()
                .map(userId -> MessageMention.builder()
                        .messageId(message.getId())
                        .conversationId(message.getConversationId())
                        .mentionedUserId(userId)
                        .build())
                .toList();

        messageMentionRepository.saveAll(mentions);

        String preview = message.getContent() != null && message.getContent().length() > 100
                ? message.getContent().substring(0, 100) + "..."
                : message.getContent();

        valid.forEach(userId ->
                messageEventPublisher.publishMentionEvent(
                        message.getId(),
                        message.getConversationId(),
                        message.getSenderId(),
                        userId,
                        preview
                )
        );

        log.info("Saved {} mention(s) for message {}", mentions.size(), message.getId());
    }
}