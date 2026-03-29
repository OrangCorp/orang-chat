package com.orang.messageservice.repository;

import com.orang.messageservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query(value = """
        SELECT 
            m.id,
            m.conversation_id,
            m.sender_id,
            m.content,
            ts_headline(
                'simple',
                m.content,
                websearch_to_tsquery('simple', :query),
                'StartSel=<mark>, StopSel=</mark>, MaxWords=2000, MinWords=1'
            ) AS highlighted_content,
            ts_rank(m.search_vector, websearch_to_tsquery('simple', :query)) AS rank,
            m.created_at
        FROM messages m
        WHERE m.conversation_id = :conversationId
          AND m.search_vector @@ websearch_to_tsquery('simple', :query)
        ORDER BY rank DESC, m.created_at DESC
        """,
            countQuery = """
            SELECT COUNT(*)
            FROM messages m
            WHERE m.conversation_id = :conversationId
              AND m.search_vector @@ websearch_to_tsquery('simple', :query)
            """,
            nativeQuery = true)
    Page<MessageRepositoryProjection> searchMessages(
            @Param("conversationId") UUID conversationId,
            @Param("query") String query,
            Pageable pageable);

    @Query(value = """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt < :timestamp
        ORDER BY m.createdAt DESC
        """)
    List<Message> findMessagesBeforeTimestamp(
            @Param("conversationId") UUID conversationId,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable pageable);

    @Query(value = """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt > :timestamp
        ORDER BY m.createdAt ASC
        """)
    List<Message> findMessagesAfterTimestamp(
            @Param("conversationId") UUID conversationId,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable pageable);

    @Query("""
        SELECT COUNT(m) > 0 FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt < :timestamp
        """)
    boolean existsOlderMessages(
            @Param("conversationId") UUID conversationId,
            @Param("timestamp") LocalDateTime timestamp);

    @Query("""
        SELECT COUNT(m) > 0 FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt > :timestamp
        """)
    boolean existsNewerMessages(
            @Param("conversationId") UUID conversationId,
            @Param("timestamp") LocalDateTime timestamp);
}
