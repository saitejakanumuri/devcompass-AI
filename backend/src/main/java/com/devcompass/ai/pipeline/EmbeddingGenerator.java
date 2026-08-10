package com.devcompass.ai.pipeline;

import com.devcompass.ai.embedding.EmbeddingProviderFactory;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingGenerator {

    private final EmbeddingProviderFactory embeddingProviderFactory;

    public EmbeddingGenerator(EmbeddingProviderFactory embeddingProviderFactory) {
        this.embeddingProviderFactory = embeddingProviderFactory;
    }

    /**
     * Generates vector embeddings delegating to the active provider (e.g. Ollama nomic-embed-text, OpenAI, or local hash).
     */
    public float[] generateEmbedding(String text) {
        return embeddingProviderFactory.getActiveProvider().embed(text);
    }

    public double calculateCosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) return 0.0;
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
