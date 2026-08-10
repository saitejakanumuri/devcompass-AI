package com.devcompass.ai.model;

import java.time.Instant;

public record SyncTrackerRecord(
    String docId,
    String docTitle,
    SourceType sourceType,
    String externalSourceId,
    Instant lastEditedTime,
    Instant lastEmbeddedDate,
    int chunkCount,
    String status
) {}
