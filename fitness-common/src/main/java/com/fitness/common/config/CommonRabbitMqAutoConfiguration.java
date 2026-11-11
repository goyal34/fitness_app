package com.fitness.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * The queue, exchange and JSON converter shared by the Activity Service
 * (publisher) and the AI Service (consumer).
 *
 * Both services declaring the same topology is harmless and idempotent. The
 * configuration is skipped when a service has no AMQP client or has not
 * configured the rabbitmq.* names, so services that only use the shared DTOs
 * are unaffected.
 */
@AutoConfiguration
@ConditionalOnClass({Queue.class, DirectExchange.class, MessageConverter.class})
@ConditionalOnProperty(prefix = "rabbitmq", name = {"queue.name", "exchange.name", "routing.key"})
public class CommonRabbitMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Queue activityQueue(@Value("${rabbitmq.queue.name}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    @ConditionalOnMissingBean
    public DirectExchange activityExchange(@Value("${rabbitmq.exchange.name}") String exchange) {
        return new DirectExchange(exchange);
    }

    @Bean
    @ConditionalOnMissingBean
    public Binding activityBinding(Queue activityQueue, DirectExchange activityExchange,
                                   @Value("${rabbitmq.routing.key}") String routingKey) {
        return BindingBuilder.bind(activityQueue).to(activityExchange).with(routingKey);
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
