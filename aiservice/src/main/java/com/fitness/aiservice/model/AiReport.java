package com.fitness.aiservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReport {
    private String summary;
    private Integer score;
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
        private String intensity;
    }
}
