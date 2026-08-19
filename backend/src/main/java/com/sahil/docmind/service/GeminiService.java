package com.sahil.docmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model}")
    private String embeddingModel;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * Converts a piece of text into an embedding vector using Gemini's embedding model.
     */
    @SuppressWarnings("unchecked")
    public List<Double> embedText(String text) {
        String url = "/" + embeddingModel + ":embedContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of("parts", List.of(Map.of("text", text)))
        );

        Map<String, Object> response = webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Gemini embedding call failed or returned no embedding. Response: " + response);
        }

        Map<String, Object> embeddingObj = (Map<String, Object>) response.get("embedding");
        List<Double> values = (List<Double>) embeddingObj.get("values");
        return values;
    }
}
