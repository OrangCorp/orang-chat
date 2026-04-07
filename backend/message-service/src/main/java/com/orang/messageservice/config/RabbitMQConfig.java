package com.orang.messageservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GROUP_EXCHANGE = "group.exchange";
    public static final String CHAT_EXCHANGE = "chat.exchange";

    // Thumbnail processing
    public static final String THUMBNAIL_QUEUE = "attachment.thumbnail.queue";
    public static final String THUMBNAIL_ROUTING_KEY = "attachment.thumbnail.requested";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange groupExchange() {
        return new TopicExchange(GROUP_EXCHANGE);
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Queue thumbnailQueue() {
        return new Queue(THUMBNAIL_QUEUE, true);
    }

    @Bean
    public Binding thumbnailBinding(Queue thumbnailQueue, TopicExchange chatExchange) {
        return BindingBuilder
                .bind(thumbnailQueue)
                .to(chatExchange)
                .with(THUMBNAIL_ROUTING_KEY);
    }
}