package com.devcompass.ai.model;

public record RegisterRequest(
    String companyName,
    String fullName,
    String email,
    String password
) {}
