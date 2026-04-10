package com.orang.messageservice.listener;

import com.orang.messageservice.service.ConversationService;
import com.orang.messageservice.service.MessageService;
import com.orang.shared.dto.ChatMessagePayload;
import com.orang.shared.dto.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageListener {

    private final MessageService messageService;
    private final ConversationService conversationService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "message.save.queue", durable = "true"),
            exchange = @Exchange(value = "chat.exchange", type = "topic"),
            key = "chat.message.sent"
    ))
    public void receiveMessage(ChatMessagePayload event) {
        log.info("Received {} message from {}", event.getType(), event.getSenderId());

        try {
            UUID conversationId;

            if (MessageType.GROUP.equals(event.getType())) {
                if (event.getConversationId() == null) {
                    throw new AmqpRejectAndDontRequeueException(
                            "Invalid GROUP message payload: conversationId is required"
                    );
                }
                conversationId = event.getConversationId();
            } else {
                if (event.getRecipientId() == null) {
                    throw new AmqpRejectAndDontRequeueException(
                            "Invalid DIRECT message payload: recipientId is required"
                    );
                }
                var conversation = conversationService.getOrCreateDirectConversation(
                        event.getSenderId(),
                        event.getRecipientId()
                );
                conversationId = conversation.getId();
            }

            messageService.saveMessage(
                    conversationId,
                    event.getSenderId(),
                    event.getContent(),
                    event.getAttachmentIds()
            );

            log.info("Message saved successfully to conversation {}", conversationId);
        } catch (AmqpRejectAndDontRequeueException e) {
            log.warn("Rejecting invalid message payload without requeue: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error saving message", e);
            throw new RuntimeException("Error saving message", e);
        }
    }
}
