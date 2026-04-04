package com.orang.chatservice.listener;

import com.orang.shared.event.MessageDeletedEvent;
import com.orang.shared.event.MessageEditedEvent;
import com.orang.shared.event.MessagePinnedEvent;
import com.orang.shared.event.MessageReactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "message.edited.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "message.edited"
    ))
    public void handleMessageEdited(MessageEditedEvent event) {
        log.info("Broadcasting message edited event for message {} in conversation {}",
                event.getMessageId(), event.getConversationId());

        // Broadcast to all users in the conversation
        messagingTemplate.convertAndSend(
                "/topic/group." + event.getConversationId(),
                event
        );
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "message.deleted.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "message.deleted"
    ))
    public void handleMessageDeleted(MessageDeletedEvent event) {
        log.info("Broadcasting message deleted event for message {} in conversation {}",
                event.getMessageId(), event.getConversationId());

        messagingTemplate.convertAndSend(
                "/topic/group." + event.getConversationId(),
                event
        );
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "message.reaction.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "message.reaction"
    ))
    public void handleMessageReaction(MessageReactionEvent event) {
        log.info("Broadcasting reaction event: {} {} on message {} in conversation {}",
                event.getAction(), event.getReactionType(),
                event.getMessageId(), event.getConversationId());

        messagingTemplate.convertAndSend(
                "/topic/group." + event.getConversationId(),
                event
        );
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "message.pin.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "message.pin"
    ))
    public void handleMessagePin(MessagePinnedEvent event) {
        log.info("Broadcasting pin event: {} for message {} in conversation {}",
                event.getAction(), event.getMessageId(), event.getConversationId());

        messagingTemplate.convertAndSend(
                "/topic/group." + event.getConversationId(),
                event
        );
    }
}