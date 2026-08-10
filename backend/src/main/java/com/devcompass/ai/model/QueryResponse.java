package com.devcompass.ai.model;

import java.util.List;

public record QueryResponse(
    String question,
    String answer,
    String providerUsed,
    List<Citation> citations,
    List<Chunk> retrievedChunks,
    long latencyMs,
    int promptTokens,
    int completionTokens
) {}
