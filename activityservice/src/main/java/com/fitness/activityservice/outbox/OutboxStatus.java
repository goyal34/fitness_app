package com.fitness.activityservice.outbox;

public enum OutboxStatus {
    /** Saved, not yet confirmed by RabbitMQ. */
    PENDING,
    /** RabbitMQ confirmed it. Removed automatically after a few days. */
    PUBLISHED,
    /** Gave up after the maximum number of attempts; needs a look. */
    FAILED
}
