package com.orang.messageservice.repository;

import com.orang.messageservice.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByMessageIdAndDeletedAtIsNull(UUID messageId);
    List<Attachment> findByConversationIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID conversationId);
    List<Attachment> findByIdIn(List<UUID> ids);

    /**
     * Find expired attachments ready for permanent deletion.
     * Used by the cleanup job.
     *
     * Finds attachments where:
     * - Soft deleted (deletedAt IS NOT NULL)
     * - Not yet permanently deleted
     * - Deleted more than 30 days ago
     */
    @Query("""
        SELECT a FROM Attachment a 
        WHERE a.deletedAt IS NOT NULL 
          AND a.permanentlyDeletedAt IS NULL
          AND a.deletedAt < :cutoffDate
        """)
    List<Attachment> findExpiredAttachments(@Param("cutoffDate") LocalDateTime messageId);

    /**
     * Find orphaned attachments (uploaded but never linked to a message).
     * Used by cleanup job to remove abandoned uploads.
     *
     * Finds attachments where:
     * - No message linked (messageId IS NULL)
     * - Not already deleted
     * - Uploaded more than 24 hours ago
     */
    @Query("""
        SELECT a FROM Attachment a
        WHERE a.messageId IS NULL
          AND a.deletedAt IS NULL
          AND a.uploadedAt < :cutoffDate
        """)
    List<Attachment> findOrphanedAttachments(@Param("cutoffDate") LocalDateTime cutoffDate);
}
