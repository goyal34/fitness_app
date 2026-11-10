package com.fitness.aiservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls Gemini with "structured output": the request includes a JSON schema,
 * and Gemini's reply is guaranteed to be JSON in exactly that shape
 * (see {@link com.fitness.aiservice.model.AiReport}).
 */
@Slf4j
@Service
public class GeminiService {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final List<String> INTENSITY = List.of("LOW", "MODERATE", "HIGH", "VERY_HIGH");
    private static final List<String> PRIORITY = List.of("HIGH", "MEDIUM", "LOW");

    /** Gemini's schema format (an OpenAPI subset). Mirrors AiReport. */
    static final Map<String, Object> REPORT_SCHEMA = object(
            Map.of(
                    "summary", string("One or two encouraging sentences summing up the workout."),
                    "score", Map.of("type", "INTEGER", "minimum", 0, "maximum", 100,
                            "description", "0-100 rating of this session for its type, duration and calories."),
                    "intensity", enumOf(INTENSITY, "How hard this session was."),
                    "analysis", array(object(Map.of(
                            "title", string("Short heading, e.g. Duration, Calorie burn, Effort."),
                            "detail", string("One or two sentences.")),
                            List.of("title", "detail")), "3 to 4 insights about the data provided."),
                    "strengths", array(string(null), "2 to 3 things that went well."),
                    "improvements", array(object(Map.of(
                            "area", string("Short name of the area."),
                            "recommendation", string("Specific, actionable advice."),
                            "priority", enumOf(PRIORITY, null)),
                            List.of("area", "recommendation", "priority")), "2 to 4 improvements."),
                    "nextWorkouts", array(object(Map.of(
                            "name", string("Workout name."),
                            "description", string("What to do, in one or two sentences."),
                            "durationMinutes", Map.of("type", "INTEGER"),
                            "intensity", enumOf(INTENSITY, null)),
                            List.of("name", "description", "durationMinutes", "intensity")), "2 to 3 suggested next sessions."),
                    "safety", array(string(null), "2 to 4 short safety tips."),
                    "recovery", array(string(null), "2 to 3 short recovery tips (hydration, food, sleep, stretching).")
            ),
            List.of("summary", "score", "intensity", "analysis", "strengths",
                    "improvements", "nextWorkouts", "safety", "recovery"));

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    void init() {
        // Older configs end the URL with "?key=" and appended the key to it.
        // The key now goes in a header so it never shows up in URLs or logs.
        geminiApiUrl = geminiApiUrl.replaceAll("[?&]key=$", "");
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set. The AI Service will start, but recommendations cannot be generated.");
        }
    }

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    /** The model name taken from the URL, e.g. "gemini-2.5-flash". */
    public String modelName() {
        int start = geminiApiUrl.indexOf("/models/");
        int end = geminiApiUrl.indexOf(':', start + 1);
        return start >= 0 && end > start ? geminiApiUrl.substring(start + 8, end) : "gemini";
    }

    /** Sends the prompt and returns Gemini's raw response body. */
    public String generateReport(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json",
                        "responseSchema", REPORT_SCHEMA));

        return webClient.post()
                .uri(geminiApiUrl)
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(TIMEOUT);
    }

    // --- small helpers to keep the schema readable ---

    private static Map<String, Object> string(String description) {
        return description == null
                ? Map.of("type", "STRING")
                : Map.of("type", "STRING", "description", description);
    }

    private static Map<String, Object> enumOf(List<String> values, String description) {
        return description == null
                ? Map.of("type", "STRING", "enum", values)
                : Map.of("type", "STRING", "enum", values, "description", description);
    }

    private static Map<String, Object> array(Map<String, Object> items, String description) {
        return Map.of("type", "ARRAY", "items", items, "description", description);
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "OBJECT", "properties", properties, "required", required);
    }
}
