package com.back.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.google-ai-gemini.chat-model.api-key:}")
    private String apiKey;

    @Value("${langchain4j.google-ai-gemini.chat-model.model-name:gemini-2.5-flash}")
    private String modelName;

    @Value("${langchain4j.google-ai-gemini.chat-model.temperature:0.2}")
    private Double temperature;

    @Value("${langchain4j.google-ai-gemini.chat-model.timeout:60s}")
    private String timeout;

    @Value("${langchain4j.google-ai-gemini.chat-model.log-requests:true}")
    private boolean logRequests;

    @Value("${langchain4j.google-ai-gemini.chat-model.log-responses:true}")
    private boolean logResponses;

    @Bean
    public ChatModel chatModel() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            log.warn("Gemini API Key is not configured. A fallback dummy ChatModel will be used.");
            return new ChatModel() {
                @Override
                public ChatResponse chat(ChatRequest request) {
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from("AI is disabled (API Key not configured)"))
                            .build();
                }
            };
        }

        // Parse timeout duration
        Duration timeoutDuration = Duration.ofSeconds(60);
        if (timeout != null && timeout.endsWith("s")) {
            try {
                timeoutDuration = Duration.ofSeconds(Long.parseLong(timeout.substring(0, timeout.length() - 1)));
            } catch (NumberFormatException ignored) {}
        }

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeoutDuration)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
