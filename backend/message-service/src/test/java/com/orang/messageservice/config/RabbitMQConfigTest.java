package com.orang.messageservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void jsonMessageConverterUsesJackson() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void exchangeAndQueueBeansUseExpectedNames() {
        TopicExchange groupExchange = config.groupExchange();
        TopicExchange chatExchange = config.chatExchange();
        Queue queue = config.thumbnailQueue();
        Binding binding = config.thumbnailBinding(queue, chatExchange);

        assertThat(groupExchange.getName()).isEqualTo(RabbitMQConfig.GROUP_EXCHANGE);
        assertThat(chatExchange.getName()).isEqualTo(RabbitMQConfig.CHAT_EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.THUMBNAIL_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.THUMBNAIL_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.CHAT_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.THUMBNAIL_ROUTING_KEY);
    }
}
