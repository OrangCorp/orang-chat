package com.orang.messageservice.repository;

import com.orang.messageservice.entity.MessageMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageMentionRepository extends JpaRepository<MessageMention, UUID> {

    @Query("SELECT m.mentionedUserId FROM MessageMention m WHERE m.messageId = :messageId")
    List<UUID> findMentionedUserIdsByMessageId(@Param("messageId") UUID messageId);

    List<MessageMention> findByMessageIdIn(Collection<UUID> messageIds);

    void deleteByMessageId(UUID messageId);
}