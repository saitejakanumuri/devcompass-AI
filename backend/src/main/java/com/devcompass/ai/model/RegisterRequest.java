package com.devcompass.ai.model;

public record RegisterRequest(
    String fullName,
    String email,
    String password
) {}
