package com.devcompass.ai.pipeline;

import com.devcompass.ai.embedding.EmbeddingProviderFactory;
import com.devcompass.ai.embedding.HashEmbeddingProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PGVectorStoreTest {

    @Test
    void testPGVectorStoreMetadata() {
        HashEmbeddingProvider hashProvider = new HashEmbeddingProvider(768);
        EmbeddingProviderFactory factory = new EmbeddingProviderFactory(List.of(hashProvider), "local-hash");
        EmbeddingGenerator embeddingGenerator = new EmbeddingGenerator(factory);

        PGVectorStore pgVectorStore = new PGVectorStore("vector_chunks", 768, embeddingGenerator);
        assertEquals("vector_chunks", pgVectorStore.getTableName());
        assertEquals("Amazon RDS PostgreSQL (pgvector)", pgVectorStore.storeName());
        assertFalse(pgVectorStore.isInitialized());
    }
}
