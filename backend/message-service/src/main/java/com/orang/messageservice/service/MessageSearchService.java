package com.orang.messageservice.service;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.dto.MessageSearchResponse;
import com.orang.messageservice.dto.MessagesAroundResponse;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.MessageRepositoryProjection;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSearchService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_PAGE_SIZE = 50;

    @Transactional(readOnly = true)
    public Page<MessageSearchResponse> searchMessages(
            UUID conversationId,
            UUID userId,
            String query,
            Pageable pageable) {

        String trimmedQuery = query.trim();
        if(trimmedQuery.length() < MIN_QUERY_LENGTH) {
            throw new BadRequestException("Query must be at least 2 characters long");
        }

        conversationService.verifyParticipant(conversationId, userId);

        Pageable clampedPageable = clampPageSize(pageable);

        Page<MessageRepositoryProjection> searchResults = messageRepository.searchMessages(
                conversationId,
                trimmedQuery,
                clampedPageable
        );

        log.debug("Search in conversation {} for query '{}': {} results",
                conversationId, trimmedQuery, searchResults.getTotalElements());

        return searchResults.map(this::toMessageSearchResponse);
    }

    @Transactional(readOnly = true)
    public MessagesAroundResponse getMessagesAround(
            UUID conversationId,
            UUID userId,
            UUID messageId,
            int numMessages) {

        conversationService.verifyParticipant(conversationId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if(!message.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Message is not part of the conversation");
        }

        int halfSize = numMessages / 2;
        int afterSize = numMessages - halfSize - 1; // -1 for the target message
        LocalDateTime timestamp = message.getCreatedAt();

        List<Message> messagesBefore = messageRepository.findMessagesBeforeTimestamp(
                conversationId,
                timestamp,
                PageRequest.of(0, halfSize)
        ).reversed();

        List<Message> messagesAfter = messageRepository.findMessagesAfterTimestamp(
                conversationId,
                timestamp,
                PageRequest.of(0, afterSize)
        );

        int targetIndex = messagesBefore.size();
        List<Message> combinedMessages = Stream.concat(
                Stream.concat(messagesBefore.stream(), Stream.of(message)),
                messagesAfter.stream()
        ).toList();

        boolean existsBefore = messageRepository.existsOlderMessages(conversationId,
                combinedMessages.getFirst().getCreatedAt());
        boolean existsAfter = messageRepository.existsNewerMessages(conversationId,
                combinedMessages.getLast().getCreatedAt());

        List<MessageResponse> messageDtos = combinedMessages.stream()
                .map(this::toMessageResponse)
                .toList();

        log.debug("Messages around {}: {} messages found", messageId, combinedMessages.size());

        return MessagesAroundResponse.builder()
                .messages(messageDtos)
                .targetMessageId(messageId)
                .targetIndex(targetIndex)
                .hasOlderMessages(existsBefore)
                .hasNewerMessages(existsAfter)
                .build();
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    private MessageSearchResponse toMessageSearchResponse(MessageRepositoryProjection result) {
        return MessageSearchResponse.builder()
                .id(result.getId())
                .conversationId(result.getConversationId())
                .senderId(result.getSenderId())
                .content(result.getContent())
                .highlightedContent(result.getHighlightedContent())
                .rank(result.getRank())
                .createdAt(result.getCreatedAt())
                .build();
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
