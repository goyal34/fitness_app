package com.fitness.aiservice.service;

import com.fitness.aiservice.model.AiReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Calls Gemini through Spring AI with structured output mapped to {@link AiReport}.
 */
@Slf4j
@Service
public class GeminiService {

    private final ObjectProvider<ChatClient> chatClient;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    public GeminiService(ObjectProvider<ChatClient> chatClient) {
        this.chatClient = chatClient;
        if (chatClient.getIfAvailable() == null) {
            log.warn("GEMINI_API_KEY is not set. The AI Service will start, but recommendations cannot be generated.");
        }
    }

    public boolean isConfigured() {
        return chatClient.getIfAvailable() != null;
    }

    public String modelName() {
        return modelName;
    }

    /** Sends the prompt and returns a typed {@link AiReport}. */
    public AiReport generateReport(String prompt) {
        ChatClient client = chatClient.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("The AI service has no Gemini API key configured.");
        }

        AiReport report = client.prompt()
                .user(prompt)
                .options(GoogleGenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.4)
                        .responseMimeType("application/json")
                        .build())
                .call()
                .entity(AiReport.class);

        if (report == null) {
            throw new IllegalStateException("Gemini returned no report.");
        }
        return report;
    }
}
