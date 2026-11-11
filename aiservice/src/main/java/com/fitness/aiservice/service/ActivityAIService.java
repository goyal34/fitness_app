package com.fitness.aiservice.service;

import com.fitness.common.event.ActivityEvent;
import com.fitness.aiservice.model.AiReport;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;

    /**
     * Asks Gemini for a structured report on the activity.
     *
     * @throws IllegalStateException if Gemini is not configured or returns no usable report
     */
    public Recommendation generateRecommendation(ActivityEvent activity) {
        if (!geminiService.isConfigured()) {
            throw new IllegalStateException("The AI service has no Gemini API key configured.");
        }
        AiReport report = clean(geminiService.generateReport(createPrompt(activity)));
        log.info("Generated report for activity {} (score {})", activity.getId(), report.getScore());

        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .status(Recommendation.STATUS_COMPLETED)
                .report(report)
                .model(geminiService.modelName())
                .recommendation(report.getSummary())
                .improvements(report.getImprovements().stream()
                        .map(i -> i.getArea() + ": " + i.getRecommendation()).toList())
                .suggestions(report.getNextWorkouts().stream()
                        .map(w -> w.getName() + ": " + w.getDescription()).toList())
                .safety(report.getSafety())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** A FAILED record, so the UI can stop waiting and explain what happened. */
    public Recommendation failedRecommendation(ActivityEvent activity, String reason) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .status(Recommendation.STATUS_FAILED)
                .errorMessage(reason)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Guards against missing or out-of-range values so the UI never has to. */
    private AiReport clean(AiReport r) {
        if (r.getSummary() == null || r.getSummary().isBlank()) {
            throw new IllegalStateException("Gemini's report had no summary.");
        }
        r.setScore(r.getScore() == null ? null : Math.max(0, Math.min(100, r.getScore())));
        r.setAnalysis(nonNull(r.getAnalysis()));
        r.setStrengths(nonNull(r.getStrengths()));
        r.setImprovements(nonNull(r.getImprovements()));
        r.setNextWorkouts(nonNull(r.getNextWorkouts()));
        r.setSafety(nonNull(r.getSafety()));
        r.setRecovery(nonNull(r.getRecovery()));
        return r;
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private String createPrompt(ActivityEvent activity) {
        Map<String, Object> metrics = activity.getAdditionalMetrics();
        return """
                You are a supportive, knowledgeable fitness coach. Write a report on the workout below
                for the person who did it. Address them as "you".

                Rules:
                - Base the analysis only on the data given. Do not invent heart rate, pace, distance
                  or other numbers that are not provided; if something useful is missing, you may
                  suggest tracking it.
                - Keep every sentence short, plain and specific. No medical diagnoses.
                - The score reflects how good this session is for its type, length and calorie burn.

                Workout:
                - Type: %s
                - Duration: %s minutes
                - Calories burned: %s
                - Started at: %s
                - Other metrics: %s
                """.formatted(
                activity.getType(),
                Objects.toString(activity.getDuration(), "unknown"),
                Objects.toString(activity.getCaloriesBurned(), "unknown"),
                Objects.toString(activity.getStartTime(), "unknown"),
                metrics == null || metrics.isEmpty() ? "none" : metrics);
    }
}
