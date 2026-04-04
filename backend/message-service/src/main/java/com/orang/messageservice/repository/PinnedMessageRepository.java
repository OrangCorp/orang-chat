package com.orang.messageservice.repository;

import com.orang.messageservice.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, UUID> {

    List<PinnedMessage> findByConversationIdOrderByPinnedAtDesc(UUID conversationId);
    Optional<PinnedMessage> findByConversationIdAndMessageId(UUID conversationId, UUID messageId);
    boolean existsByConversationIdAndMessageId(UUID conversationId, UUID messageId);
    long countByConversationId(UUID conversationId);
}
