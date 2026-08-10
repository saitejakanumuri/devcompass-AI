package com.devcompass.ai.source;

import com.devcompass.ai.model.SourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KnowledgeSourceRegistry {

    private final Map<SourceType, KnowledgeSource> sources = new ConcurrentHashMap<>();

    public KnowledgeSourceRegistry(List<KnowledgeSource> sourceList) {
        for (KnowledgeSource source : sourceList) {
            sources.put(source.type(), source);
        }
    }

    public List<KnowledgeSource> getAllSources() {
        return List.copyOf(sources.values());
    }

    public Optional<KnowledgeSource> getSource(SourceType type) {
        return Optional.ofNullable(sources.get(type));
    }
}
