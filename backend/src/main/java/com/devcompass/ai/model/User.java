package com.devcompass.ai.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String email,
    String passwordHash,
    String fullName,
    String role,
    Instant createdAt,
    Instant updatedAt
) {}
