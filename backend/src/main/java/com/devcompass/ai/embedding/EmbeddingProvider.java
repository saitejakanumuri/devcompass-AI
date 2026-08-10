package com.devcompass.ai.embedding;

public interface EmbeddingProvider {

    /**
     * Generates vector embeddings for a given text input.
     */
    float[] embed(String text);

    /**
     * Unique identifier of the embedding provider (e.g. ollama, openai, local-hash).
     */
    String providerName();

    /**
     * Embedding vector dimension (e.g. 768 for nomic-embed-text, 1536 for text-embedding-3-small).
     */
    int dimension();
}
