package com.fitness.aiservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "recommendations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;
    private String activityId;
    private String userId;
    private String activityType;

    /** COMPLETED, or FAILED when Gemini could not produce a report. */
    private String status;
    /** Short, user-safe reason when status is FAILED. */
    private String errorMessage;

    /** The structured report. Missing on reports created before it existed. */
    private AiReport report;
    private String model;

    // Flat text versions, kept for older reports and simple clients.
    private String recommendation;
    private List<String> improvements;
    private List<String> suggestions;
    private List<String> safety;

    @CreatedDate
    private LocalDateTime createdAt;
}
