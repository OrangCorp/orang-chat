package com.orang.messageservice.listener;

import com.orang.messageservice.dto.ChatMessageEvent;
import com.orang.messageservice.service.ConversationService;
import com.orang.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
    public void receiveMessage(ChatMessageEvent event) {
        log.info("Received message from {} to {}", event.getSenderId(), event.getRecipientId());

        try {
            var conversation = conversationService.getOrCreateDirectConversation(
                    event.getSenderId(),
                    event.getRecipientId()
            );

            messageService.saveMessage(conversation.getId(), event.getSenderId(), event.getContent());

            log.info("Message saved successfully");
        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage());
        }
    }
}
