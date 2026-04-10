package com.orang.messageservice.repository;

import com.orang.messageservice.dto.ReactionCountProjection;
import com.orang.messageservice.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);
    List<MessageReaction> findByMessageId(UUID messageId);

    @Query("""
            SELECT r.reactionType AS reactionType, COUNT(r) AS count
            FROM MessageReaction r
            WHERE r.messageId = :messageId
            GROUP BY r.reactionType
            """)
    List<ReactionCountProjection> countByMessageIdGroupByType(@Param("messageId") UUID messageId);

    void deleteByMessageIdAndUserId(UUID messageId, UUID userId);
}
