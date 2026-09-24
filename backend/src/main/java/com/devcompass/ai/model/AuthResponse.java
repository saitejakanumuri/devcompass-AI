package com.devcompass.ai.model;

import java.util.UUID;

public record AuthResponse(
    String token,
    UUID userId,
    String fullName,
    String email,
    String role
) {}
