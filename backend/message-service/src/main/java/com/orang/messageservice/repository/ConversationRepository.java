package com.orang.messageservice.repository;

import com.orang.messageservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p " +
            "WHERE p.id.userId = :userId")
    List<Conversation> findByParticipantIdsContaining(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p1 " +
            "JOIN c.participants p2 " +
            "WHERE c.type = 'DIRECT' " +
            "AND p1.id.userId = :userId1 " +
            "AND p2.id.userId = :userId2")
    Optional<Conversation> findDirectConversationBetween(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2);
}