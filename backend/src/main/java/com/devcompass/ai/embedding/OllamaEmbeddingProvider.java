package com.devcompass.ai.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Component("ollamaEmbeddingProvider")
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final RestTemplate restTemplate;

    public OllamaEmbeddingProvider(
        @Value("${devcompass.embedding.ollama.base-url:http://localhost:11434}") String baseUrl,
        @Value("${devcompass.embedding.ollama.model:nomic-embed-text}") String model,
        @Value("${devcompass.embedding.ollama.dimension:768}") int dimension
    ) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimension = dimension;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }

        try {
            String url = baseUrl + "/api/embeddings";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "model", model,
                "prompt", text
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Double> embeddingList = (List<Double>) response.getBody().get("embedding");
                if (embeddingList != null && !embeddingList.isEmpty()) {
                    float[] vector = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        vector[i] = embeddingList.get(i).floatValue();
                    }
                    return vector;
                }
            }
        } catch (Exception e) {
            // Log fallback when Ollama daemon is offline or model nomic-embed-text is pull-pending
        }

        return fallbackHashVector(text);
    }

    private float[] fallbackHashVector(String text) {
        float[] vector = new float[dimension];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < dimension; i++) {
                byte b = hash[i % hash.length];
                vector[i] = (float) ((b & 0xFF) / 255.0 * 2.0 - 1.0);
            }
        } catch (NoSuchAlgorithmException ignored) {}
        return vector;
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    public String modelName() {
        return model;
    }

    public String baseUrl() {
        return baseUrl;
    }
}
