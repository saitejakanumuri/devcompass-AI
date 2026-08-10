package com.devcompass.ai.model;

import java.util.Map;

public record Citation(
    String id,
    String sourceTitle,
    SourceType sourceType,
    String documentPath,
    String snippet,
    double relevanceScore,
    Map<String, Object> metadata
) {}
