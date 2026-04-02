package com.orang.messageservice.service;

import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.PinnedMessage;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.PinnedMessageRepository;
import com.orang.shared.event.MessagePinnedEvent;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinnedMessageService {

    private final PinnedMessageRepository pinnedMessageRepository;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageEventPublisher messageEventPublisher;

    @Transactional
    public void pinMessage(UUID conversationId, UUID messageId, UUID userId) {
        conversationService.verifyParticipant(conversationId, userId);

        Message message = messageRepository.findActiveById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        if (pinnedMessageRepository.existsByConversationIdAndMessageId(conversationId, messageId)) {
            log.debug("Message {} already pinned in conversation {}", messageId, conversationId);
            return;
        }

        PinnedMessage pin = PinnedMessage.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .pinnedBy(userId)
                .build();

        pinnedMessageRepository.save(pin);
        log.info("User {} pinned message {} in conversation {}", userId, messageId, conversationId);

        messageEventPublisher.publishMessagePinChanged(
                messageId,
                conversationId,
                userId,
                MessagePinnedEvent.Action.PINNED
        );
    }

    @Transactional
    public void unpinMessage(UUID conversationId, UUID messageId, UUID userId) {
        PinnedMessage pin = pinnedMessageRepository
                .findByConversationIdAndMessageId(conversationId, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message is not pinned"));

        boolean isPinner = pin.getPinnedBy().equals(userId);
        boolean isAdmin = conversationService.isAdmin(conversationId, userId);

        if (!isPinner && !isAdmin) {
            throw new ForbiddenException("Only the pinner or an admin can unpin this message");
        }

        pinnedMessageRepository.delete(pin);
        log.info("User {} unpinned message {} in conversation {}", userId, messageId, conversationId);

        messageEventPublisher.publishMessagePinChanged(
                messageId,
                conversationId,
                userId,
                MessagePinnedEvent.Action.UNPINNED
        );
    }

    public List<UUID> getPinnedMessageIds(UUID conversationId, UUID userId) {
        conversationService.verifyParticipant(conversationId, userId);

        return pinnedMessageRepository
                .findByConversationIdOrderByPinnedAtDesc(conversationId)
                .stream()
                .map(PinnedMessage::getMessageId)
                .toList();
    }

    public boolean isPinned(UUID conversationId, UUID messageId) {
        return pinnedMessageRepository.existsByConversationIdAndMessageId(conversationId, messageId);
    }
}