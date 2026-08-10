package com.devcompass.ai.model;

import java.time.Instant;
import java.util.List;

public record IngestionResult(
    SourceType sourceType,
    int documentsProcessed,
    int totalChunksGenerated,
    int vectorEmbeddingsCreated,
    String status,
    List<String> logs,
    Instant timestamp
) {}
