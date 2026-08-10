package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class InMemoryVectorStore implements VectorStore {

    private final List<Chunk> storage = new ArrayList<>();
    private final EmbeddingGenerator embeddingGenerator;

    public InMemoryVectorStore(EmbeddingGenerator embeddingGenerator) {
        this.embeddingGenerator = embeddingGenerator;
    }

    @Override
    public synchronized void saveChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;
        storage.addAll(chunks);
    }

    @Override
    public List<Chunk> similaritySearch(String queryText, int topK, double threshold) {
        if (storage.isEmpty() || queryText == null || queryText.isBlank()) {
            return List.of();
        }

        float[] queryVector = embeddingGenerator.generateEmbedding(queryText);
        String lowerQuery = queryText.toLowerCase();

        return storage.stream()
            .map(chunk -> {
                double vectorSim = embeddingGenerator.calculateCosineSimilarity(queryVector, chunk.embedding());
                // Hybrid boost for keyword matching in content/title
                double textMatchBoost = 0.0;
                if (chunk.content().toLowerCase().contains(lowerQuery)) textMatchBoost += 0.35;
                if (chunk.documentTitle().toLowerCase().contains(lowerQuery)) textMatchBoost += 0.25;
                
                // Specific keyword boosts
                if (lowerQuery.contains("seo") && chunk.content().toLowerCase().contains("seo")) textMatchBoost += 0.40;
                if (lowerQuery.contains("tutor") && chunk.content().toLowerCase().contains("tutor")) textMatchBoost += 0.30;
                if (lowerQuery.contains("deploy") && chunk.content().toLowerCase().contains("deploy")) textMatchBoost += 0.40;
                if (lowerQuery.contains("auth") && chunk.content().toLowerCase().contains("auth")) textMatchBoost += 0.40;
                if (lowerQuery.contains("table") && chunk.content().toLowerCase().contains("table")) textMatchBoost += 0.40;

                double finalScore = Math.min(0.99, vectorSim * 0.4 + textMatchBoost * 0.6);
                return chunk.withScore(finalScore);
            })
            .sorted(Comparator.comparingDouble(Chunk::score).reversed())
            .limit(topK)
            .toList();
    }

    @Override
    public int totalIndexedChunks() {
        return storage.size();
    }

    @Override
    public synchronized void clearStore() {
        storage.clear();
    }

    @Override
    public synchronized void deleteChunksByDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) return;
        storage.removeIf(chunk -> documentId.equals(chunk.documentId()));
    }
}
