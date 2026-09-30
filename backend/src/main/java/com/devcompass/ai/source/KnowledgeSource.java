package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;

import java.util.List;

public interface KnowledgeSource {

    /**
     * Synchronizes and extracts documents from the underlying engineering platform using the provided config.
     */
    List<Document> sync(java.util.Map<String, Object> config);

    /**
     * The category of knowledge source (NOTION, GIT_REPOSITORY, DATABASE_METADATA).
     */
    SourceType type();

    /**
     * Human readable name of the source connection.
     */
    String sourceName(java.util.Map<String, Object> config);

    /**
     * Returns true if connection credentials and API endpoints are valid.
     */
    boolean isHealthy(java.util.Map<String, Object> config);

    /**
     * Helper to safely extract and default string values from a config map.
     */
    default String str(java.util.Map<String, Object> map, String key, String defaultVal) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) return defaultVal;
        String val = map.get(key).toString().trim();
        return val.isEmpty() ? defaultVal : val;
    }
}
