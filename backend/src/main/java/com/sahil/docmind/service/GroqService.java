package com.sahil.docmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Handles the "generation" half of the RAG pipeline using Groq's free,
 * OpenAI-compatible chat completion API. Embeddings still come from Gemini
 * (GeminiService) since that part already works and Groq doesn't offer
 * an embedding endpoint.
 */
@Service
public class GroqService {

    private final WebClient webClient;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    public GroqService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * Sends a prompt (question + retrieved context) to Groq and returns the generated answer.
     */
    @SuppressWarnings("unchecked")
    public String generateAnswer(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Groq generation call failed or returned no choices. Response: " + response);
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
