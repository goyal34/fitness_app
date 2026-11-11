package com.fitness.activityservice.outbox;

import com.fitness.common.event.ActivityEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A message waiting to be sent to RabbitMQ (Transactional Outbox pattern).
 *
 * It is saved in the same MongoDB transaction as the activity it describes,
 * so either both exist or neither does. {@link OutboxPublisher} later sends
 * it and marks it PUBLISHED once RabbitMQ confirms it.
 */
@Document(collection = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    public static final String ACTIVITY_CREATED = "ACTIVITY_CREATED";

    @Id
    private String id;
    private String eventType;
    /** Id of the activity the event is about. */
    private String aggregateId;
    /** The message body sent to RabbitMQ. */
    private ActivityEvent payload;

    private OutboxStatus status;
    private int attempts;
    /** When the publisher may next try this event (also used as a short lease while sending). */
    private Instant nextAttemptAt;
    private String lastError;
    private Instant createdAt;
    private Instant publishedAt;

    public static OutboxEvent activityCreated(ActivityEvent activity) {
        Instant now = Instant.now();
        return OutboxEvent.builder()
                .eventType(ACTIVITY_CREATED)
                .aggregateId(activity.getId())
                .payload(activity)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
    }
}
