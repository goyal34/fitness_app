package com.fitness.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The RabbitMQ topology must be declared only where the rabbitmq.* names exist,
 * because every service depends on fitness-common (including the gateway, which
 * has no AMQP client).
 */
class CommonRabbitMqAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonRabbitMqAutoConfiguration.class));

    @Test
    void declaresTheSharedTopologyWhenConfigured() {
        runner.withPropertyValues(
                        "rabbitmq.queue.name=activity.queue",
                        "rabbitmq.exchange.name=fitness.exchange",
                        "rabbitmq.routing.key=activity.tracking")
                .run(context -> {
                    assertThat(context).hasSingleBean(Queue.class);
                    assertThat(context).hasSingleBean(DirectExchange.class);
                    assertThat(context).hasSingleBean(Binding.class);
                    assertThat(context).hasSingleBean(MessageConverter.class);
                    assertThat(context.getBean(Queue.class).getName()).isEqualTo("activity.queue");
                    assertThat(context.getBean(DirectExchange.class).getName()).isEqualTo("fitness.exchange");
                });
    }

    @Test
    void backsOffWhenTheServiceHasNoRabbitMqConfiguration() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(Queue.class);
            assertThat(context).doesNotHaveBean(MessageConverter.class);
        });
    }
}
