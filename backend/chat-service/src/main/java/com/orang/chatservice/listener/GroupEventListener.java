package com.orang.chatservice.listener;

import com.orang.shared.event.GroupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "chat.group.events")
    public void handleGroupEvent(GroupEvent event) {
        log.info("Received {}: {}", event.getClass().getSimpleName(), event);

        String destination = "/topic/group." + event.getConversationId();

        messagingTemplate.convertAndSend(destination, event);

        log.info("Broadcast to {}", destination);
    }
}