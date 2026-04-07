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
        log.info("Broadcasting thumbnail ready for attachment {} in conversation {}",
                event.getAttachmentId(), event.getConversationId());

        messagingTemplate.convertAndSend(
                "/topic/group." + event.getConversationId(),
                Map.of(
                        "type", "THUMBNAIL_READY",
                        "attachmentId", event.getAttachmentId().toString(),
                        "messageId", event.getMessageId() != null ? event.getMessageId().toString() : null,
                        "thumbnailUrl", event.getThumbnailUrl()
                )
        );
    }
}