package com.orang.chatservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange groupExchange() {
        return new TopicExchange("group.exchange");
    }

    @Bean
    public Queue groupEventsQueue() {
        return new Queue("chat.group.events", true);  // durable queue
    }

    @Bean
    public Binding groupEventsBinding(TopicExchange groupExchange, Queue groupEventsQueue) {
        return BindingBuilder.bind(groupEventsQueue)
                .to(groupExchange)
                .with("group.#");  // # matches everything after "group."
    }
}