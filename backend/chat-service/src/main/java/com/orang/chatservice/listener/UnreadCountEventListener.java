package com.orang.chatservice.listener;

import com.orang.shared.event.UnreadCountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnreadCountEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "chat.notification.unread")
    public void handleUnreadCount(UnreadCountEvent event) {
        log.debug("Broadcasting unread count {} to user {}",
                event.getUnreadCount(), event.getUserId());

        // convertAndSendToUser sends ONLY to this specific user
        // The client subscribes to: /user/queue/notifications/unread
        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/notifications/unread",
                Map.of("unreadCount", event.getUnreadCount())
        );
    }
}