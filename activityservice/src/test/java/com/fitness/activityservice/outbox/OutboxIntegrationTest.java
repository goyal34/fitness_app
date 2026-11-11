package com.fitness.activityservice.outbox;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.ActivityService;
import com.fitness.activityservice.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Transactional Outbox, tested against a real MongoDB replica set and a real
 * RabbitMQ broker (Docker, via Testcontainers).
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/unused",
        "spring.rabbitmq.publisher-confirm-type=correlated",
        "spring.rabbitmq.publisher-returns=true",
        "spring.rabbitmq.template.mandatory=true",
        "rabbitmq.exchange.name=fitness.exchange",
        "rabbitmq.queue.name=activity.queue",
        "rabbitmq.routing.key=activity.tracking",
        // Tests trigger the publisher themselves.
        "outbox.publisher.interval-ms=3600000",
        "outbox.publisher.max-attempts=3",
})
class OutboxIntegrationTest {

    private static final String QUEUE = "activity.queue";

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7"); // starts as a replica set

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management")
            .withStartupTimeout(Duration.ofMinutes(3)); // can take over a minute to boot on Docker Desktop

    @Autowired ActivityService activityService;
    @Autowired OutboxPublisher publisher;
    @Autowired ActivityRepository activityRepository;
    @Autowired MongoTemplate mongoTemplate;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin rabbitAdmin;
    @Autowired Queue activityQueue;
    @Autowired DirectExchange activityExchange;
    @Autowired Binding activityBinding;

    @MockitoBean UserValidationService userValidationService;
    @MockitoSpyBean OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void reset() {
        when(userValidationService.validateUser(anyString())).thenReturn(true);
        activityRepository.deleteAll();
        mongoTemplate.remove(new Query(), OutboxEvent.class);
        rabbitAdmin.declareQueue(activityQueue);
        rabbitAdmin.declareBinding(activityBinding);
        rabbitAdmin.purgeQueue(QUEUE, false);
    }

    @Test
    void savesActivityAndPendingEventTogether_thenPublishesAfterConfirm() {
        ActivityResponse created = activityService.trackActivity(request());

        List<OutboxEvent> events = mongoTemplate.findAll(OutboxEvent.class);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(events.get(0).getAggregateId()).isEqualTo(created.getId());

        publisher.publishPending();

        OutboxEvent published = mongoTemplate.findById(events.get(0).getId(), OutboxEvent.class);
        assertThat(published.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();

        Activity message = (Activity) rabbitTemplate.receiveAndConvert(QUEUE, 5000);
        assertThat(message).isNotNull();
        assertThat(message.getId()).isEqualTo(created.getId());
        assertThat(message.getUserId()).isEqualTo("user-1");
    }

    @Test
    void rollsBackTheActivityWhenTheEventCannotBeSaved() {
        doThrow(new IllegalStateException("simulated failure"))
                .when(outboxEventRepository).save(any(OutboxEvent.class));

        assertThatThrownBy(() -> activityService.trackActivity(request()))
                .hasMessageContaining("simulated failure");

        assertThat(activityRepository.count()).isZero();
        assertThat(mongoTemplate.count(new Query(), OutboxEvent.class)).isZero();
    }

    @Test
    void keepsTheEventPendingWhileRabbitCannotDeliverIt_thenSendsItLater() {
        activityService.trackActivity(request());

        // No queue bound: RabbitMQ returns the message as unroutable.
        rabbitAdmin.deleteQueue(QUEUE);
        publisher.publishPending();

        OutboxEvent failed = mongoTemplate.findOne(new Query(), OutboxEvent.class);
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("unroutable");
        assertThat(failed.getNextAttemptAt()).isAfter(Instant.now()); // backing off

        // Broker is fine again, and the backoff has passed.
        rabbitAdmin.declareQueue(activityQueue);
        rabbitAdmin.declareBinding(activityBinding);
        makeDueNow();
        publisher.publishPending();

        assertThat(mongoTemplate.findOne(new Query(), OutboxEvent.class).getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(rabbitTemplate.receiveAndConvert(QUEUE, 5000)).isNotNull();
    }

    @Test
    void marksTheEventFailedAfterMaxAttempts() {
        activityService.trackActivity(request());
        rabbitAdmin.deleteQueue(QUEUE);

        for (int i = 0; i < 3; i++) {
            makeDueNow();
            publisher.publishPending();
        }

        OutboxEvent event = mongoTemplate.findOne(new Query(), OutboxEvent.class);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
    }

    private void makeDueNow() {
        mongoTemplate.updateMulti(Query.query(where("status").is(OutboxStatus.PENDING)),
                new Update().set("nextAttemptAt", Instant.now()), OutboxEvent.class);
    }

    private static ActivityRequest request() {
        ActivityRequest request = new ActivityRequest();
        request.setUserId("user-1");
        request.setType(ActivityType.RUNNING);
        request.setDuration(30);
        request.setCaloriesBurned(300);
        return request;
    }
}
