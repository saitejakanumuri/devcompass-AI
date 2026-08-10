package com.devcompass.ai.pipeline;

import com.devcompass.ai.embedding.EmbeddingProviderFactory;
import com.devcompass.ai.embedding.HashEmbeddingProvider;
import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PGVectorStoreTest {

    @Test
    void testPGVectorStoreFallbackAndSearch() {
        HashEmbeddingProvider hashProvider = new HashEmbeddingProvider(768);
        EmbeddingProviderFactory factory = new EmbeddingProviderFactory(List.of(hashProvider), "local-hash");
        EmbeddingGenerator embeddingGenerator = new EmbeddingGenerator(factory);
        InMemoryVectorStore delegateStore = new InMemoryVectorStore(embeddingGenerator);

        PGVectorStore pgVectorStore = new PGVectorStore("vector_chunks", 768, delegateStore, embeddingGenerator);
        assertEquals("vector_chunks", pgVectorStore.getTableName());

        float[] embedding = hashProvider.embed("Tutor SEO metadata table");
        Chunk chunk = new Chunk(
            "chunk-1", "doc-1", "SEO Doc", SourceType.NOTION,
            "Tutor SEO metadata table definition in PostgreSQL",
            embedding, 12, Map.of(), 0.0
        );

        pgVectorStore.saveChunks(List.of(chunk));
        assertTrue(pgVectorStore.totalIndexedChunks() > 0);

        List<Chunk> results = pgVectorStore.similaritySearch("Tutor SEO metadata", 3, 0.3);
        assertFalse(results.isEmpty());
        assertEquals("chunk-1", results.get(0).id());
    }
}
