package com.orang.userservice.listener;

import com.orang.shared.event.UserRegisteredEvent;
import com.orang.userservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final ProfileService profileService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "user.registered.queue", durable = "true"),
            exchange = @Exchange(value = "user.exchange", type = "topic"),
            key = "user.registered"
    ))
    public void userRegisteredEvent(UserRegisteredEvent event) {
        log.info("User registered event received: userId={}", event.getUserId());

        try {
            profileService.createProfileIfNotExists(event.getUserId(), event.getDisplayName());
        } catch (Exception e) {
            log.error("Failed to create profile for userId={}: {}",
                    event.getUserId(),
                    e.getMessage(),
                    e);
            // Rethrow to trigger RabbitMQ retry/DLQ
            throw new AmqpRejectAndDontRequeueException(
                    "Failed to process UserRegisteredEvent for userId=" + event.getUserId(),
                    e
            );
        }
    }
}
