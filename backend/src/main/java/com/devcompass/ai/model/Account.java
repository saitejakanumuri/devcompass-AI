package com.devcompass.ai.model;

import java.time.Instant;
import java.util.UUID;

public record Account(
    UUID id,
    String companyName,
    String accountKey,
    Instant createdAt,
    Instant updatedAt
) {}
