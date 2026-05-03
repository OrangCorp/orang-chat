package com.orang.notificationservice.config;

import com.orang.shared.constants.RabbitMQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String MENTION_NOTIFICATION_QUEUE = "notification.mention";

    // Exchanges (must match what publishers use)
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String GROUP_EXCHANGE = "group.exchange";

    // Queue names (our own queues for notification service)
    public static final String MESSAGE_NOTIFICATION_QUEUE = "notification.message.sent";
    public static final String REACTION_NOTIFICATION_QUEUE = "notification.reaction";
    public static final String MEMBER_ADDED_NOTIFICATION_QUEUE = "notification.member.added";
    public static final String DIRECT_CONVERSATION_CREATED_NOTIFICATION_QUEUE =
            RabbitMQConstants.DIRECT_CONVERSATION_CREATED_NOTIFICATION_QUEUE;

    // Routing keys (must match what publishers use!)
    public static final String MESSAGE_SENT_ROUTING_KEY = "message.sent";
    public static final String REACTION_ROUTING_KEY = "message.reaction";
    public static final String MEMBER_ADDED_ROUTING_KEY = "group.member.added";
    public static final String MENTION_ROUTING_KEY = "message.mention";
    public static final String DIRECT_CONVERSATION_CREATED_ROUTING_KEY =
            RabbitMQConstants.DIRECT_CONVERSATION_CREATED_KEY;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    // =========================================================================
    // Exchanges
    // =========================================================================

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange groupExchange() {
        return new TopicExchange(GROUP_EXCHANGE, true, false);
    }

    // =========================================================================
    // Queues
    // =========================================================================

    @Bean
    public Queue messageNotificationQueue() {
        return QueueBuilder.durable(MESSAGE_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue reactionNotificationQueue() {
        return QueueBuilder.durable(REACTION_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue memberAddedNotificationQueue() {
        return QueueBuilder.durable(MEMBER_ADDED_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue mentionNotificationQueue() {
        return QueueBuilder.durable(MENTION_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue directConversationCreatedNotificationQueue() {
        return QueueBuilder.durable(DIRECT_CONVERSATION_CREATED_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue contactRequestNotificationQueue() {
        return QueueBuilder.durable(RabbitMQConstants.CONTACT_REQUEST_NOTIFICATION_QUEUE).build();
    }

    // =========================================================================
    // Bindings
    // =========================================================================

    @Bean
    public Binding messageNotificationBinding() {
        return BindingBuilder
                .bind(messageNotificationQueue())
                .to(chatExchange())
                .with(MESSAGE_SENT_ROUTING_KEY);
    }

    @Bean
    public Binding reactionNotificationBinding() {
        return BindingBuilder
                .bind(reactionNotificationQueue())
                .to(chatExchange())
                .with(REACTION_ROUTING_KEY);
    }

    @Bean
    public Binding memberAddedNotificationBinding() {
        return BindingBuilder
                .bind(memberAddedNotificationQueue())
                .to(groupExchange())
                .with(MEMBER_ADDED_ROUTING_KEY);
    }

    @Bean
    public Binding mentionNotificationBinding() {
        return BindingBuilder
                .bind(mentionNotificationQueue())
                .to(chatExchange())
                .with(MENTION_ROUTING_KEY);
    }

    @Bean
    public Binding directConversationCreatedNotificationBinding() {
        return BindingBuilder
                .bind(directConversationCreatedNotificationQueue())
                .to(chatExchange())
                .with(DIRECT_CONVERSATION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding contactRequestNotificationBinding() {
        return BindingBuilder
                .bind(contactRequestNotificationQueue())
                .to(contactExchange())
                .with(RabbitMQConstants.CONTACT_REQUEST_SENT_KEY);
    }

    @Bean
    public TopicExchange contactExchange() {
        return new TopicExchange(RabbitMQConstants.CONTACT_EXCHANGE, true, false);
    }
}