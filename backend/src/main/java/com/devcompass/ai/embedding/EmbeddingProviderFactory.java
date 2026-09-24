package com.devcompass.ai.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmbeddingProviderFactory {

    private final Map<String, EmbeddingProvider> providers = new ConcurrentHashMap<>();
    private String activeProviderName;

    public EmbeddingProviderFactory(
        List<EmbeddingProvider> providerList,
        @Value("${devcompass.embedding.provider:ollama}") String defaultProvider
    ) {
        for (EmbeddingProvider provider : providerList) {
            this.providers.put(provider.providerName().toLowerCase(), provider);
        }
        this.activeProviderName = defaultProvider.toLowerCase();
    }

    public EmbeddingProvider getActiveProvider() {
        return getProvider(activeProviderName);
    }

    public EmbeddingProvider getProvider(String name) {
        if (name == null || name.isBlank()) {
            return getActiveProvider();
        }
        return Optional.ofNullable(providers.get(name.toLowerCase()))
            .orElseGet(this::getActiveProvider);
    }
}
