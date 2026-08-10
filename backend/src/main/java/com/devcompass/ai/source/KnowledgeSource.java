package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;

import java.util.List;

public interface KnowledgeSource {

    /**
     * Synchronizes and extracts documents from the underlying engineering platform.
     */
    List<Document> sync();

    /**
     * The category of knowledge source (NOTION, GIT_REPOSITORY, DATABASE_METADATA).
     */
    SourceType type();

    /**
     * Human readable name of the source connection.
     */
    String sourceName();

    /**
     * Returns true if connection credentials and API endpoints are valid.
     */
    boolean isHealthy();
}
