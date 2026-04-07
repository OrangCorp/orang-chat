package com.orang.messageservice.listener;

import com.orang.messageservice.config.RabbitMQConfig;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.event.ThumbnailRequestedEvent;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThumbnailEventListener {

    private final ThumbnailService thumbnailService;
    private final AttachmentRepository attachmentRepository;

    @RabbitListener(queues = RabbitMQConfig.THUMBNAIL_QUEUE)
    @Transactional
    public void handleThumbnailRequest(ThumbnailRequestedEvent event) {
        log.info("Processing thumbnail request for attachment {}", event.getAttachmentId());

        if (!thumbnailService.supportsFileType(event.getFileType())) {
            log.debug("Skipping thumbnail - unsupported file type: {}", event.getFileType());
            return;
        }

        Attachment attachment = attachmentRepository.findById(event.getAttachmentId())
                .orElse(null);

        if (attachment == null) {
            log.warn("Attachment {} not found, skipping thumbnail", event.getAttachmentId());
            return;
        }

        if (attachment.isDeleted()) {
            log.debug("Attachment {} is deleted, skipping thumbnail", event.getAttachmentId());
            return;
        }

        if (attachment.getThumbnailGenerated()) {
            log.debug("Attachment {} already has thumbnail, skipping", event.getAttachmentId());
            return;
        }

        String thumbnailStorageKey = thumbnailService.generateAndUploadThumbnail(
                event.getStorageKey(),
                event.getConversationId(),
                event.getAttachmentId()
        );

        if (thumbnailStorageKey != null) {
            attachment.setThumbnailStorageKey(thumbnailStorageKey);
            attachment.setThumbnailGenerated(true);
            attachmentRepository.save(attachment);

            log.info("Thumbnail saved for attachment {}", event.getAttachmentId());
        } else {
            log.warn("Thumbnail generation failed for attachment {}", event.getAttachmentId());
        }
    }
}