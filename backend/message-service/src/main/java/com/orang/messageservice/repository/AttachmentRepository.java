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
    List<Attachment> findExpiredAttachments(@Param("cutoffDate") LocalDateTime cutoffDate);

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

    /**
     * Find attachments that failed thumbnail generation but can still retry.
     * Used by scheduled retry job.
     *
     * Criteria:
     * - Thumbnail not generated yet
     * - Has at least 1 failed attempt
     * - Less than 3 total attempts
     * - Last attempt was more than backoffMinutes ago
     */
    @Query("""
    SELECT a FROM Attachment a
    WHERE a.thumbnailGenerated = FALSE
      AND a.thumbnailAttempts > 0
      AND a.thumbnailAttempts < 3
      AND a.thumbnailLastAttempt < :cutoffTime
      AND a.deletedAt IS NULL
    """)
    List<Attachment> findAttachmentsNeedingThumbnailRetry(@Param("cutoffTime") LocalDateTime cutoffTime);
}
