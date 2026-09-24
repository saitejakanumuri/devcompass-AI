package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core ingestion pipeline: documents → chunks → embeddings → vector store.
 * Always scoped to a single user — no global ingestion.
 */
@Service
public class IngestionPipelineService {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipelineService.class);

    private final ChunkGenerator chunkGenerator;
    private final EmbeddingGenerator embeddingGenerator;
    private final VectorStore vectorStore;

    public IngestionPipelineService(
        ChunkGenerator chunkGenerator,
        EmbeddingGenerator embeddingGenerator,
        VectorStore vectorStore
    ) {
        this.chunkGenerator = chunkGenerator;
        this.embeddingGenerator = embeddingGenerator;
        this.vectorStore = vectorStore;
    }

    /**
     * Ingest documents for a specific user. Chunks old data, generates fresh embeddings.
     */
    public IngestionResult ingestDocumentsForAccount(UUID userId, SourceType sourceType, List<Document> documents) {
        List<String> logs = new ArrayList<>();
        logs.add("[" + Instant.now() + "] Ingesting " + documents.size() + " docs for userId=" + userId + " type=" + sourceType);

        int totalChunks = 0;
        int totalEmbeddings = 0;
        List<Chunk> allChunks = new ArrayList<>();

        for (Document doc : documents) {
            String docId = (doc.sourceId() != null && !doc.sourceId().isBlank()) ? doc.sourceId() : doc.id();

            // Remove old chunks for this document
            vectorStore.deleteChunksByDocumentId(docId);

            List<String> sections = chunkGenerator.splitIntoSections(doc);
            int chunkIndex = 0;

            for (String sectionText : sections) {
                float[] embedding = embeddingGenerator.generateEmbedding(sectionText);
                totalEmbeddings++;
                totalChunks++;

                Chunk chunk = new Chunk(
                    docId + "-chunk-" + (++chunkIndex),
                    docId,
                    doc.title(),
                    doc.sourceType(),
                    sectionText,
                    embedding,
                    Math.max(1, sectionText.length() / 4),
                    doc.metadata(),
                    0.0
                );
                allChunks.add(chunk);
            }
        }

        vectorStore.saveChunks(allChunks);
        logs.add("Indexed " + totalChunks + " chunks with " + totalEmbeddings + " embeddings.");
        logs.add("Status: SUCCESS");

        log.info("[Pipeline] Ingested {} chunks for userId={} type={}", totalChunks, userId, sourceType);

        return new IngestionResult(sourceType, documents.size(), totalChunks, totalEmbeddings, "SUCCESS", logs, Instant.now());
    }
}
