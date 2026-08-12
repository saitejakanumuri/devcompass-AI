package com.devcompass.ai.model;

import java.util.UUID;

public record AuthResponse(
    String token,
    UUID userId,
    UUID accountId,
    String companyName,
    String fullName,
    String email,
    String role
) {}
