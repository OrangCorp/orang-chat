package com.orang.messageservice.scheduler;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentCleanupJob {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    /**
     * Permanently deletes attachments that were soft-deleted 30+ days ago.
     * Runs daily at 2 AM server time.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredAttachments() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Attachment> expiredAttachments = attachmentRepository.findExpiredAttachments(cutoffDate);

        log.info("Starting cleanup of {} expired attachments", expiredAttachments.size());

        int successCount = 0;
        int failCount = 0;

        for (Attachment attachment : expiredAttachments) {
            try {
                fileStorageService.deleteFile(attachment.getStorageKey());

                if (attachment.getThumbnailStorageKey() != null) {
                    fileStorageService.deleteFile(attachment.getThumbnailStorageKey());
                }

                attachment.setPermanentlyDeletedAt(LocalDateTime.now());
                attachmentRepository.save(attachment);

                successCount++;

                log.debug("Permanently deleted attachment {} (file: {})",
                        attachment.getId(), attachment.getFileName());

            } catch (Exception e) {
                failCount++;
                log.error("Failed to delete attachment {} from storage: {}",
                        attachment.getId(), e.getMessage(), e);
            }
        }

        log.info("Cleanup completed: {} succeeded, {} failed out of {} total",
                successCount, failCount, expiredAttachments.size());
    }

    /**
     * Deletes orphaned attachments (uploaded but never linked to a message).
     * Runs daily at 3 AM server time.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanedAttachments() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        List<Attachment> orphanedAttachments = attachmentRepository.findOrphanedAttachments(cutoffDate);

        log.info("Starting cleanup of {} orphaned attachments", orphanedAttachments.size());

        int successCount = 0;
        int failCount = 0;

        for (Attachment attachment : orphanedAttachments) {
            try {
                if (!attachment.isOrphaned(24)) {
                    log.warn("Attachment {} no longer orphaned, skipping", attachment.getId());
                    continue;

                }
                fileStorageService.deleteFile(attachment.getStorageKey());

                if (attachment.getThumbnailStorageKey() != null) {
                    fileStorageService.deleteFile(attachment.getThumbnailStorageKey());
                }

                attachmentRepository.delete(attachment);

                successCount++;

                log.debug("Deleted orphaned attachment {} (file: {}, uploaded: {})",
                        attachment.getId(), attachment.getFileName(), attachment.getUploadedAt());

            } catch (Exception e) {
                failCount++;
                log.error("Failed to delete orphaned attachment {}: {}",
                        attachment.getId(), e.getMessage(), e);
            }
        }

        log.info("Orphan cleanup completed: {} succeeded, {} failed out of {} total",
                successCount, failCount, orphanedAttachments.size());
    }
}