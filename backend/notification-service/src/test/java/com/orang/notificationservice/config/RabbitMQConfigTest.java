package com.orang.notificationservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    @DisplayName("rabbitmq bean methods expose the expected queues, exchanges, and bindings")
    void rabbitmqBeanMethodsExposeExpectedQueuesExchangesAndBindings() {
        RabbitMQConfig config = new RabbitMQConfig();
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);
        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);

        assertThat(config.rabbitListenerContainerFactory(connectionFactory)).isNotNull();

        TopicExchange chatExchange = config.chatExchange();
        TopicExchange groupExchange = config.groupExchange();
        TopicExchange contactExchange = config.contactExchange();
        Queue messageQueue = config.messageNotificationQueue();
        Queue reactionQueue = config.reactionNotificationQueue();
        Queue memberQueue = config.memberAddedNotificationQueue();
        Queue mentionQueue = config.mentionNotificationQueue();
        Queue directQueue = config.directConversationCreatedNotificationQueue();
        Queue contactQueue = config.contactRequestNotificationQueue();
        Binding messageBinding = config.messageNotificationBinding();
        Binding reactionBinding = config.reactionNotificationBinding();
        Binding memberBinding = config.memberAddedNotificationBinding();
        Binding mentionBinding = config.mentionNotificationBinding();
        Binding directBinding = config.directConversationCreatedNotificationBinding();
        Binding contactBinding = config.contactRequestNotificationBinding();

        assertThat(chatExchange.getName()).isEqualTo(RabbitMQConfig.CHAT_EXCHANGE);
        assertThat(groupExchange.getName()).isEqualTo(RabbitMQConfig.GROUP_EXCHANGE);
        assertThat(contactExchange.getName()).isEqualTo(com.orang.shared.constants.RabbitMQConstants.CONTACT_EXCHANGE);
        assertThat(messageQueue.getName()).isEqualTo(RabbitMQConfig.MESSAGE_NOTIFICATION_QUEUE);
        assertThat(reactionQueue.getName()).isEqualTo(RabbitMQConfig.REACTION_NOTIFICATION_QUEUE);
        assertThat(memberQueue.getName()).isEqualTo(RabbitMQConfig.MEMBER_ADDED_NOTIFICATION_QUEUE);
        assertThat(mentionQueue.getName()).isEqualTo(RabbitMQConfig.MENTION_NOTIFICATION_QUEUE);
        assertThat(directQueue.getName()).isEqualTo(RabbitMQConfig.DIRECT_CONVERSATION_CREATED_NOTIFICATION_QUEUE);
        assertThat(contactQueue.getName()).isEqualTo(com.orang.shared.constants.RabbitMQConstants.CONTACT_REQUEST_NOTIFICATION_QUEUE);
        assertThat(messageBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.MESSAGE_SENT_ROUTING_KEY);
        assertThat(reactionBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.REACTION_ROUTING_KEY);
        assertThat(memberBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.MEMBER_ADDED_ROUTING_KEY);
        assertThat(mentionBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.MENTION_ROUTING_KEY);
        assertThat(directBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.DIRECT_CONVERSATION_CREATED_ROUTING_KEY);
        assertThat(contactBinding.getRoutingKey()).isEqualTo(com.orang.shared.constants.RabbitMQConstants.CONTACT_REQUEST_SENT_KEY);
    }
}