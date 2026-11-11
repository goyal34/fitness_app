package com.fitness.aiservice.config;

import com.google.genai.Client;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Spring AI's Google GenAI {@link ChatClient} only when a Gemini API key
 * is present, so the service can still start without one (recommendations then
 * fail with a clear FAILED report).
 *
 * Spring AI auto-config is disabled via {@code spring.ai.model.chat=none}
 * because it refuses to start with an empty key.
 */
@Configuration
public class GeminiAiConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${gemini.api.key:}')")
    ChatModel googleGenAiChatModel(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model:gemini-2.5-flash}") String model) {
        Client client = Client.builder().apiKey(apiKey).build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.4)
                        .responseMimeType("application/json")
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${gemini.api.key:}')")
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
