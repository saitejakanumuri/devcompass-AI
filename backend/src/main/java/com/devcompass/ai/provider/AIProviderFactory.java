package com.devcompass.ai.provider;

import com.devcompass.ai.model.ProviderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
            .orElseThrow(() -> new IllegalArgumentException("Unknown AI Provider: " + name + ". Available: " + providers.keySet()));
    }

    public void setActiveProvider(String providerName) {
        if (!providers.containsKey(providerName.toLowerCase())) {
            throw new IllegalArgumentException("Cannot activate unknown provider: " + providerName);
        }
        this.activeProviderName = providerName.toLowerCase();
    }

    public List<ProviderConfig> listProviders() {
        return providers.values().stream()
            .map(p -> new ProviderConfig(
                p.providerName(),
                p.providerName().toUpperCase() + " Provider",
                p.modelName(),
                p.providerName(),
                p.providerName().equalsIgnoreCase(activeProviderName),
                "Provider implementation handling RAG prompt construction and inference.",
                p.contextWindowSize(),
                0.2
            ))
            .toList();
    }
}
