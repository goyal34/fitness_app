package com.fitness.activityservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Sends pending outbox events to RabbitMQ.
 *
 * - Each event is claimed atomically (findAndModify pushes its nextAttemptAt
 *   forward as a lease), so several instances of this service never send the
 *   same event at the same time.
 * - An event is marked PUBLISHED only after RabbitMQ confirms it (publisher
 *   confirms) and did not return it as unroutable.
 * - Failures are retried with exponential backoff, then marked FAILED.
 *
 * Delivery is at-least-once: if the service stops between the confirm and
 * the PUBLISHED update, the event is sent again. The AI Service ignores
 * activities it has already reported on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration LEASE = Duration.ofSeconds(30);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final MongoTemplate mongoTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-attempts:10}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:2000}")
    public void publishPending() {
        for (int i = 0; i < batchSize; i++) {
            OutboxEvent event = claimNext();
            if (event == null) {
                return;
            }
            publish(event);
        }
    }

    private OutboxEvent claimNext() {
        Instant now = Instant.now();
        Query due = Query.query(where("status").is(OutboxStatus.PENDING).and("nextAttemptAt").lte(now))
                .with(Sort.by("createdAt"));
        Update lease = new Update().set("nextAttemptAt", now.plus(LEASE));
        return mongoTemplate.findAndModify(due, lease, FindAndModifyOptions.options().returnNew(true), OutboxEvent.class);
    }

    private void publish(OutboxEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.getId());
            rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload(), correlation);

            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            // The returned message (if any) is always set before the confirm arrives.
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("Message was unroutable: " + correlation.getReturned().getReplyText());
            }
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected the message: " + confirm.getReason());
            }
            markPublished(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markAttemptFailed(event, e);
        } catch (Exception e) {
            markAttemptFailed(event, e);
        }
    }

    private void markPublished(OutboxEvent event) {
        mongoTemplate.updateFirst(byId(event),
                new Update()
                        .set("status", OutboxStatus.PUBLISHED)
                        .set("publishedAt", Instant.now())
                        .inc("attempts", 1)
                        .unset("lastError"),
                OutboxEvent.class);
        log.info("Published {} for activity {}", event.getEventType(), event.getAggregateId());
    }

    private void markAttemptFailed(OutboxEvent event, Exception error) {
        int attempts = event.getAttempts() + 1;
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        Update update = new Update().set("attempts", attempts).set("lastError", message);

        if (attempts >= maxAttempts) {
            update.set("status", OutboxStatus.FAILED);
            log.error("Giving up on outbox event {} for activity {} after {} attempts: {}",
                    event.getId(), event.getAggregateId(), attempts, message);
        } else {
            Duration backoff = Duration.ofSeconds(1L << Math.min(attempts, 20));
            if (backoff.compareTo(MAX_BACKOFF) > 0) {
                backoff = MAX_BACKOFF;
            }
            update.set("nextAttemptAt", Instant.now().plus(backoff));
            log.warn("Could not publish outbox event {} (attempt {}), retrying in {}s: {}",
                    event.getId(), attempts, backoff.toSeconds(), message);
        }
        mongoTemplate.updateFirst(byId(event), update, OutboxEvent.class);
    }

    private static Query byId(OutboxEvent event) {
        return Query.query(where("_id").is(event.getId()));
    }
}
