package com.devcompass.ai.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingProviderTest {

    @Test
    void testOllamaEmbeddingProviderFallback() {
        OllamaEmbeddingProvider ollama = new OllamaEmbeddingProvider("http://localhost:11434", "nomic-embed-text", 768);
        assertEquals("ollama", ollama.providerName());
        assertEquals(768, ollama.dimension());

        float[] vector = ollama.embed("SEO application flow architecture");
        assertNotNull(vector);
        assertEquals(768, vector.length);
    }

    @Test
    void testEmbeddingProviderFactory() {
        OllamaEmbeddingProvider ollama = new OllamaEmbeddingProvider("http://localhost:11434", "nomic-embed-text", 768);
        OpenAIEmbeddingProvider openai = new OpenAIEmbeddingProvider("text-embedding-3-small", 1536);
        HashEmbeddingProvider hash = new HashEmbeddingProvider(768);

        EmbeddingProviderFactory factory = new EmbeddingProviderFactory(List.of(ollama, openai, hash), "ollama");

        assertEquals("ollama", factory.getActiveProvider().providerName());
        assertEquals(768, factory.getActiveProvider().dimension());

        factory.setActiveProvider("openai");
        assertEquals("openai", factory.getActiveProvider().providerName());
    }
}
