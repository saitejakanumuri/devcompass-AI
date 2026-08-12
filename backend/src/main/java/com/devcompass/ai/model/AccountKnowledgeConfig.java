package com.devcompass.ai.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a user's per-source knowledge configuration (formerly AccountKnowledgeConfig).
 * Keyed by user_id — each user manages their own source connections.
 */
public record AccountKnowledgeConfig(
    UUID id,
    UUID userId,
    SourceType sourceType,
    Map<String, Object> configJson,
    String status,
    Instant lastSyncedAt,
    Instant createdAt,
    Instant updatedAt
) {}
