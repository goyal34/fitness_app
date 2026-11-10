package com.fitness.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The structured workout report produced by Gemini. Its shape matches the
 * response schema sent in {@link com.fitness.aiservice.service.GeminiService},
 * so Gemini's JSON maps straight onto it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReport {
    /** One or two sentence headline about the workout. */
    private String summary;
    /** 0-100 rating of the session given its type, duration and calories. */
    private Integer score;
    /** LOW, MODERATE, HIGH or VERY_HIGH. */
    private String intensity;
    private List<Insight> analysis;
    private List<String> strengths;
    private List<Improvement> improvements;
    private List<Workout> nextWorkouts;
    private List<String> safety;
    private List<String> recovery;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Insight {
        private String title;
        private String detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Improvement {
        private String area;
        private String recommendation;
        /** HIGH, MEDIUM or LOW. */
        private String priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Workout {
        private String name;
        private String description;
        private Integer durationMinutes;
        /** LOW, MODERATE, HIGH or VERY_HIGH. */
        private String intensity;
    }
}
