package com.devcompass.ai.model;

public record QueryRequest(
    String question,
    String provider,
    Integer topK,
    Double temperature,
    String systemPromptOverride
) {
    public QueryRequest {
        if (topK == null) topK = 5;
        if (temperature == null) temperature = 0.2;
    }
}
