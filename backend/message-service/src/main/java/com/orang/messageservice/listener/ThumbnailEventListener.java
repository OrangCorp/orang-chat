package com.orang.messageservice.listener;

import com.orang.messageservice.config.RabbitMQConfig;
import com.orang.messageservice.entity.Attachment;
import com.orang.shared.event.ThumbnailReadyEvent;
import com.orang.messageservice.event.ThumbnailRequestedEvent;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThumbnailEventListener {

    private final ThumbnailService thumbnailService;
    private final AttachmentRepository attachmentRepository;
    private final RabbitTemplate rabbitTemplate;

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

        if (thumbnailStorageKey == null) {
            log.warn("Thumbnail generation failed for attachment {}", event.getAttachmentId());
            return;
        }

        attachment.setThumbnailStorageKey(thumbnailStorageKey);
        attachment.setThumbnailGenerated(true);
        attachmentRepository.save(attachment);

        log.info("Thumbnail saved for attachment {}", event.getAttachmentId());

        // Publish thumbnail ready event for real-time updates
        ThumbnailReadyEvent readyEvent = ThumbnailReadyEvent.builder()
                .attachmentId(attachment.getId())
                .conversationId(attachment.getConversationId())
                .messageId(attachment.getMessageId())
                .thumbnailUrl("/api/attachments/" + attachment.getId() + "/thumbnail")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.THUMBNAIL_READY_ROUTING_KEY,
                readyEvent
        );

        log.debug("Published thumbnail ready event for attachment {}", attachment.getId());
    }
}