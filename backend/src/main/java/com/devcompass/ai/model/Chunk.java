package com.devcompass.ai.model;

import java.util.Map;

public record Chunk(
    String id,
    String documentId,
    String documentTitle,
    SourceType sourceType,
    String content,
    float[] embedding,
    int tokenCount,
    Map<String, Object> metadata,
    double score
) {
    public Chunk withScore(double score) {
        return new Chunk(id, documentId, documentTitle, sourceType, content, embedding, tokenCount, metadata, score);
    }
}
