package com.devcompass.ai.model;

import java.time.Instant;
import java.util.Map;

public record Document(
    String id,
    String title,
    String sourceId,
    SourceType sourceType,
    String content,
    Map<String, Object> metadata,
    Instant createdAt
) {
    public Document(String id, String title, String sourceId, SourceType sourceType, String content, Map<String, Object> metadata) {
        this(id, title, sourceId, sourceType, content, metadata, Instant.now());
    }
}
