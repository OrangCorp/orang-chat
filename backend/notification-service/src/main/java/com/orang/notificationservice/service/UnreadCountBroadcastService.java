package com.orang.notificationservice.service;

import com.orang.notificationservice.config.RabbitMQConfig;
import com.orang.shared.event.UnreadCountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCountBroadcastService {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationPersistenceService persistenceService;

    public void broadcast(UUID userId) {
        long count = persistenceService.getUnreadCount(userId);

        UnreadCountEvent event = UnreadCountEvent.builder()
                .userId(userId)
                .unreadCount(count)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.UNREAD_COUNT_ROUTING_KEY,
                event
        );

        log.debug("Broadcast unread count {} for user {}", count, userId);
    }
}