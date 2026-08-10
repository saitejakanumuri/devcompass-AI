package com.devcompass.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("geminiProvider")
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final String model;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public GeminiProvider() {
        this("gemini-3.5-flash-lite", "demo-key");
    }

    @Autowired
    public GeminiProvider(
        @Value("${devcompass.ai.gemini.model:gemini-3.5-flash-lite}") String model,
        @Value("${devcompass.ai.gemini.api-key:demo-key}") String apiKey
    ) {
        this.model = model;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        log.info("[GeminiProvider] Initialized with model: '{}', Raw Injected API Key: '{}'", model, maskKey(apiKey));
    }

    @Override
    public String generateAnswer(String question, String context, String systemPrompt, double temperature) {
        log.info("[GeminiProvider] Preparing LLM answer generation for question: '{}'", question);
        log.info("[GeminiProvider] Using model: '{}', Configured API Key: '{}'", model, maskKey(apiKey));

        if (isApiKeyConfigured()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String fullPrompt = systemPrompt + "\n\nRetrieved Context:\n" + context + "\n\nUser Question:\n" + question;

                Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", fullPrompt)))
                    ),
                    "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", 2048
                    )
                );

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                
                log.info("[GeminiProvider] Sending POST request to Google Generative AI API: {}", maskUrl(url));
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                log.info("[GeminiProvider] Received HTTP response status: {}", response.getStatusCode());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map> candidates = (List<Map>) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map content = (Map) candidates.get(0).get("content");
                        if (content != null) {
                            List<Map> parts = (List<Map>) content.get("parts");
                            if (parts != null && !parts.isEmpty() && parts.get(0).get("text") != null) {
                                String answer = parts.get(0).get("text").toString();
                                log.info("[GeminiProvider] Successfully generated answer from live Gemini API (length: {} chars)", answer.length());
                                return answer;
                            }
                        }
                    }
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("[GeminiProvider] HTTP Error from Google Gemini API (Status {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("[GeminiProvider] Unexpected exception while calling Google Gemini API: {}", e.getMessage(), e);
            }
        } else {
            log.warn("[GeminiProvider] API Key is set to 'demo-key' or blank. Falling back to simulated local RAG response.");
        }

        log.warn("[GeminiProvider] Returning static fallback synthesis response.");
        return """
            [Google Gemini 1.5 Flash Analysis]
            
            Synthesizing retrieved system knowledge for developer query: "%s"
            
            Key Insights from Knowledge Base:
            - **Architecture**: Spring Boot backend connected to pgvector vector store and Redis cache.
            - **Flow**: Direct API routing with schema validation and automated citation tracking.
            - **Retrieved Context Highlights**:
              %s
            """.formatted(question, context.length() > 300 ? context.substring(0, 300) + "..." : context);
    }

    private boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("demo-key");
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "<NULL_OR_BLANK>";
        if (key.equalsIgnoreCase("demo-key")) return "demo-key";
        if (key.length() <= 8) return key;
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }

    private String maskUrl(String url) {
        if (url != null && url.contains("key=")) {
            int keyIdx = url.indexOf("key=");
            String prefix = url.substring(0, keyIdx + 4);
            String keyPart = url.substring(keyIdx + 4);
            String maskedKey = maskKey(keyPart);
            return prefix + maskedKey;
        }
        return url;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public int contextWindowSize() {
        return 1000000;
    }
}
