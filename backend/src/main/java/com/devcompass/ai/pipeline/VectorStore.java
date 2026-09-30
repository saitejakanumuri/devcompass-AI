package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;

import java.util.List;

public interface VectorStore {

    void saveChunks(List<Chunk> chunks);

    void saveChunks(List<Chunk> chunks, java.util.UUID userId);

    List<Chunk> similaritySearch(String queryText, int topK, double threshold);

    List<Chunk> similaritySearch(String queryText, int topK, double threshold, java.util.UUID userId);

    int totalIndexedChunks();

    void clearStore();

    void clearStoreForUser(java.util.UUID userId);

    void deleteChunksBySourceType(com.devcompass.ai.model.SourceType sourceType, java.util.UUID userId);

    default String storeName() {
        return "Amazon RDS PostgreSQL (pgvector)";
    }

    default boolean isInitialized() {
        return true;
    }

    default String tableName() {
        return "vector_chunks";
    }
}
