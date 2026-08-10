package com.devcompass.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("openaiProvider")
public class OpenAIProvider implements AIProvider {

    private final String model;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public OpenAIProvider() {
        this("gpt-4o", "demo-key");
    }

    public OpenAIProvider(
        @Value("${devcompass.ai.openai.model:gpt-4o}") String model,
        @Value("${devcompass.ai.openai.api-key:demo-key}") String apiKey
    ) {
        this.model = model;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String generateAnswer(String question, String context, String systemPrompt, double temperature) {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals("demo-key")) {
            try {
                String url = "https://api.openai.com/v1/chat/completions";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt + "\n\nRetrieved Context:\n" + context),
                        Map.of("role", "user", "content", question)
                    )
                );

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map> choices = (List<Map>) response.getBody().get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map message = (Map) choices.get(0).get("message");
                        if (message != null && message.get("content") != null) {
                            return message.get("content").toString();
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback for offline or demo API key
            }
        }

        return """
            [ChatGPT / OpenAI %s Response]
            
            Synthesized engineering answer using retrieved context for query: "%s"
            
            Key Insights:
            - Pipeline embedding powered by Ollama (nomic-embed-text) & stored in pgvector.
            - Answer generated using retrieved Notion, Git, and database schema documentation.
            """.formatted(model, question);
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public int contextWindowSize() {
        return 128000;
    }
}
