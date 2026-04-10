package com.orang.messageservice.scheduler;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.ThumbnailService;
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
public class ThumbnailRetryJob {

    private final AttachmentRepository attachmentRepository;
    private final ThumbnailService thumbnailService;

    private static final int RETRY_BACKOFF_MINUTES = 30;
    private static final int MAX_RETRIES_PER_RUN = 20;

    /**
     * Retries failed thumbnail generations every 30 minutes.
     * Only retries attachments that:
     * - Failed at least once
     * - Have less than 3 attempts
     * - Last attempt was more than 30 minutes ago
     *
     * Processes max 20 attachments per run to prevent resource spikes.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void retryFailedThumbnails() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(RETRY_BACKOFF_MINUTES);

        List<Attachment> attachments = attachmentRepository
                .findAttachmentsNeedingThumbnailRetry(cutoffTime)
                .stream()
                .limit(MAX_RETRIES_PER_RUN)
                .toList();

        if (attachments.isEmpty()) {
            log.debug("No thumbnails need retry");
            return;
        }

        log.info("Retrying thumbnail generation for {} attachments (max {})",
                attachments.size(), MAX_RETRIES_PER_RUN);

        int successCount = 0;
        int failCount = 0;

        for (Attachment attachment : attachments) {
            try {
                String thumbnailStorageKey = thumbnailService.generateAndUploadThumbnail(
                        attachment.getStorageKey(),
                        attachment.getConversationId(),
                        attachment.getId()
                );

                if (thumbnailStorageKey != null) {
                    attachment.markThumbnailSuccess(thumbnailStorageKey);
                    attachmentRepository.save(attachment);
                    successCount++;
                    log.debug("Retry succeeded for attachment {}", attachment.getId());
                } else {
                    attachment.recordThumbnailAttempt("Retry returned null");
                    attachmentRepository.save(attachment);
                    failCount++;
                }

            } catch (Exception e) {
                attachment.recordThumbnailAttempt(e.getMessage());
                attachmentRepository.save(attachment);
                failCount++;
                log.error("Retry failed for attachment {}: {}", attachment.getId(), e.getMessage());
            }
        }

        log.info("Thumbnail retry completed: {} succeeded, {} failed out of {} processed",
                successCount, failCount, attachments.size());
    }
}