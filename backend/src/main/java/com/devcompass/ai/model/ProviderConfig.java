package com.devcompass.ai.model;

public record ProviderConfig(
    String id,
    String name,
    String model,
    String providerType,
    boolean active,
    String description,
    int contextWindow,
    double defaultTemperature
) {}
