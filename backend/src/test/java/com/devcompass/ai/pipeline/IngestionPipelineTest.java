package com.devcompass.ai.pipeline;

import com.devcompass.ai.embedding.EmbeddingProviderFactory;
import com.devcompass.ai.embedding.HashEmbeddingProvider;
import com.devcompass.ai.embedding.OllamaEmbeddingProvider;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.source.DatabaseKnowledgeSource;
import com.devcompass.ai.source.GitKnowledgeSource;
import com.devcompass.ai.source.KnowledgeSourceRegistry;
import com.devcompass.ai.source.NotionKnowledgeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngestionPipelineTest {

    @Test
    void testFullPipelineIngestion() {
        KnowledgeSourceRegistry registry = new KnowledgeSourceRegistry(List.of(
            new NotionKnowledgeSource(),
            new GitKnowledgeSource(),
            new DatabaseKnowledgeSource()
        ));
        DocumentExtractor extractor = new DocumentExtractor();
        ChunkGenerator chunkGenerator = new ChunkGenerator(512, 64);
        
        OllamaEmbeddingProvider ollama = new OllamaEmbeddingProvider("http://localhost:11434", "nomic-embed-text", 768);
        HashEmbeddingProvider hash = new HashEmbeddingProvider(768);
        EmbeddingProviderFactory embeddingFactory = new EmbeddingProviderFactory(List.of(ollama, hash), "ollama");

        EmbeddingGenerator embeddingGenerator = new EmbeddingGenerator(embeddingFactory);
        InMemoryVectorStore vectorStore = new InMemoryVectorStore(embeddingGenerator);

        IngestionPipelineService service = new IngestionPipelineService(
            registry, extractor, chunkGenerator, embeddingGenerator, vectorStore
        );

        List<IngestionResult> results = service.runFullIngestionPipeline();
        assertEquals(3, results.size());
        assertTrue(vectorStore.totalIndexedChunks() > 0);

        List<Chunk> retrieved = vectorStore.similaritySearch("SEO flow", 3, 0.5);
        assertFalse(retrieved.isEmpty());
    }
}
