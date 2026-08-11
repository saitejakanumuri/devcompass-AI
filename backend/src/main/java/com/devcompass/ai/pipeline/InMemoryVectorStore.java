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
        String[] queryWords = lowerQuery.split("[\\s\\p{Punct}]+");

        return storage.stream()
            .map(chunk -> {
                double vectorSim = embeddingGenerator.calculateCosineSimilarity(queryVector, chunk.embedding());
                String lowerContent = chunk.content().toLowerCase();
                String lowerTitle = chunk.documentTitle().toLowerCase();

                // Dynamic keyword matching across all tokens in user query
                double textMatchBoost = 0.0;
                for (String word : queryWords) {
                    if (word.length() >= 2) {
                        if (lowerTitle.contains(word)) {
                            textMatchBoost += 0.35;
                        } else if (lowerContent.contains(word)) {
                            textMatchBoost += 0.20;
                        }
                    }
                }

                double normalizedTextBoost = Math.min(1.0, textMatchBoost);
                // If zero query tokens match title or content, apply damping penalty to baseline vector noise
                double adjustedVectorSim = (textMatchBoost == 0.0) ? vectorSim * 0.35 : vectorSim;
                double finalScore = Math.min(0.99, Math.max(0.0, adjustedVectorSim * 0.4 + normalizedTextBoost * 0.6));
                return chunk.withScore(finalScore);
            })
            .filter(chunk -> chunk.score() >= threshold)
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
