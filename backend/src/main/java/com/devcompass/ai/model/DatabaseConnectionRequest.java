package com.devcompass.ai.model;

public record DatabaseConnectionRequest(
    String url,
    String username,
    String password,
    String driverClassName
) {}
