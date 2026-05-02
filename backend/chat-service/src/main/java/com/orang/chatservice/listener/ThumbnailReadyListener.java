package com.orang.chatservice.listener;

import com.orang.shared.event.ThumbnailReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThumbnailReadyListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "attachment.thumbnail.ready.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "attachment.thumbnail.ready"
    ))
    public void handleThumbnailReady(ThumbnailReadyEvent event) {
        try {
            log.info("Broadcasting thumbnail ready for attachment {} in conversation {}",
                    event.getAttachmentId(), event.getConversationId());

            // Use HashMap instead of Map.of() to allow null values
            Map<String, String> payload = new HashMap<>();
            payload.put("type", "THUMBNAIL_READY");
            payload.put("attachmentId", event.getAttachmentId() != null ? event.getAttachmentId().toString() : null);
            payload.put("messageId", event.getMessageId() != null ? event.getMessageId().toString() : null);
            payload.put("thumbnailUrl", event.getThumbnailUrl());

            messagingTemplate.convertAndSend(
                    "/topic/group." + event.getConversationId(),
                    payload
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast thumbnail ready event: {}", e.getMessage());
            // Don't retry
        }
    }
}