package com.devcompass.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that manages available LLM providers (Gemini, OpenAI, Claude, Ollama).
 * Uses config-driven default provider — no UI switching needed.
 */
@Component
public class AIProviderFactory {

    private final Map<String, AIProvider> providers = new ConcurrentHashMap<>();
    private String activeProviderName;

    public AIProviderFactory(List<AIProvider> providerList,
                             @Value("${devcompass.ai.provider:gemini}") String defaultProvider) {
        for (AIProvider provider : providerList) {
            this.providers.put(provider.providerName().toLowerCase(), provider);
        }
        this.activeProviderName = defaultProvider.toLowerCase();
    }

    public AIProvider getActiveProvider() {
        return getProvider(activeProviderName);
    }

    public AIProvider getProvider(String name) {
        if (name == null || name.isBlank()) {
            return getActiveProvider();
        }
        return Optional.ofNullable(providers.get(name.toLowerCase()))
            .orElseGet(this::getActiveProvider);
    }
}
