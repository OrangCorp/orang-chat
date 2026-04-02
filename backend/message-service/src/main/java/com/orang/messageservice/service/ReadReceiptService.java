package com.orang.messageservice.service;

import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.ReadReceipt;
import com.orang.messageservice.entity.ReadReceiptId;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.ReadReceiptRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadReceiptService {

    private final ReadReceiptRepository readReceiptRepository;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    @Transactional
    public void markAsRead(UUID userId, UUID conversationId, UUID messageId) {
        conversationService.verifyParticipant(conversationId, userId);

        Message message = messageRepository.findActiveById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        ReadReceiptId id = ReadReceiptId.builder()
                .userId(userId)
                .conversationId(conversationId)
                .build();

        Optional<ReadReceipt> existingReceipt = readReceiptRepository.findById(id);

        if (existingReceipt.isPresent()) {
            ReadReceipt receipt = existingReceipt.get();

            Message currentLastRead = messageRepository.findById(receipt.getLastReadMessageId())
                    .orElse(null);

            if (currentLastRead == null ||
                    message.getCreatedAt().isAfter(currentLastRead.getCreatedAt())) {
                receipt.updatePointer(messageId);
                readReceiptRepository.save(receipt);
                log.info("Updated read pointer for user {} in conversation {} to message {}",
                        userId, conversationId, messageId);
            } else {
                log.debug("Ignoring older message as read pointer for user {} in conversation {}",
                        userId, conversationId);
            }
        } else {
            ReadReceipt newReceipt = ReadReceipt.builder()
                    .id(id)
                    .lastReadMessageId(messageId)
                    .readAt(LocalDateTime.now())
                    .build();
            readReceiptRepository.save(newReceipt);
            log.info("Created read pointer for user {} in conversation {} at message {}",
                    userId, conversationId, messageId);
        }
    }

    public long getUnreadCount(UUID userId, UUID conversationId) {
        Optional<ReadReceipt> receipt = readReceiptRepository.findById(
                ReadReceiptId.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build()
        );

        return receipt.map(readReceipt -> messageRepository.countUnreadMessages(
                conversationId,
                readReceipt.getLastReadMessageId()
        )).orElseGet(() -> messageRepository.countActiveMessagesByConversationId(conversationId));
    }

    public Optional<UUID> getLastReadMessageId(UUID userId, UUID conversationId) {
        return readReceiptRepository.findById(
                ReadReceiptId.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build()
        ).map(ReadReceipt::getLastReadMessageId);
    }
}