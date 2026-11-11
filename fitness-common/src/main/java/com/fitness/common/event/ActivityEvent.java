package com.fitness.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The message the Activity Service publishes to RabbitMQ and the AI Service
 * consumes. It is deliberately a plain DTO rather than the Activity document,
 * so the wire contract is independent of how either service stores data.
 *
 * {@code type} is the activity type name (for example "RUNNING"), which keeps
 * this module free of the Activity Service's enum.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityEvent {
    private String id;
    private String userId;
    private String type;
    private Integer duration;
    private Integer caloriesBurned;
    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
