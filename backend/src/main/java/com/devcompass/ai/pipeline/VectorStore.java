package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;

import java.util.List;

public interface VectorStore {

    void saveChunks(List<Chunk> chunks);

    List<Chunk> similaritySearch(String queryText, int topK, double threshold);

    int totalIndexedChunks();

    void clearStore();

    void deleteChunksByDocumentId(String documentId);

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
