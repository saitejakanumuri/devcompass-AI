package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.source.KnowledgeSource;
import com.devcompass.ai.source.KnowledgeSourceRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionPipelineService {

    private final KnowledgeSourceRegistry sourceRegistry;
    private final DocumentExtractor documentExtractor;
    private final ChunkGenerator chunkGenerator;
    private final EmbeddingGenerator embeddingGenerator;
    private final VectorStore vectorStore;

    public IngestionPipelineService(
        KnowledgeSourceRegistry sourceRegistry,
        DocumentExtractor documentExtractor,
        ChunkGenerator chunkGenerator,
        EmbeddingGenerator embeddingGenerator,
        VectorStore vectorStore
    ) {
        this.sourceRegistry = sourceRegistry;
        this.documentExtractor = documentExtractor;
        this.chunkGenerator = chunkGenerator;
        this.embeddingGenerator = embeddingGenerator;
        this.vectorStore = vectorStore;
    }

    public List<IngestionResult> runFullIngestionPipeline() {
        List<IngestionResult> results = new ArrayList<>();
        vectorStore.clearStore();

        for (KnowledgeSource source : sourceRegistry.getAllSources()) {
            results.add(ingestFromSource(source));
        }

        return results;
    }

    public IngestionResult ingestSourceType(SourceType sourceType) {
        return sourceRegistry.getSource(sourceType)
            .map(this::ingestFromSource)
            .orElseThrow(() -> new IllegalArgumentException("Unknown source type: " + sourceType));
    }

    private IngestionResult ingestFromSource(KnowledgeSource source) {
        List<String> logs = new ArrayList<>();
        logs.add("[" + Instant.now() + "] Triggering KnowledgeSource sync for: " + source.sourceName());

        List<Document> documents = source.sync();
        logs.add("Extracted " + documents.size() + " document artifacts.");

        int totalChunks = 0;
        int totalEmbeddings = 0;
        List<Chunk> allSourceChunks = new ArrayList<>();

        for (Document doc : documents) {
            List<String> sections = chunkGenerator.splitIntoSections(doc);
            int chunkIndex = 0;

            for (String sectionText : sections) {
                // Generate a distinct, section-specific vector embedding for THIS chunk content
                float[] chunkVector = embeddingGenerator.generateEmbedding(sectionText);
                totalEmbeddings++;
                totalChunks++;

                int approxTokens = Math.max(1, sectionText.length() / 4);
                Chunk chunk = new Chunk(
                    doc.id() + "-chunk-" + (++chunkIndex),
                    doc.id(),
                    doc.title(),
                    doc.sourceType(),
                    sectionText,
                    chunkVector,
                    approxTokens,
                    doc.metadata(),
                    0.0
                );
                allSourceChunks.add(chunk);
            }
        }

        vectorStore.saveChunks(allSourceChunks);
        logs.add("Generated " + totalChunks + " distinct text chunks with section-specific vector embeddings into pgvector.");
        logs.add("Status: SUCCESS");

        return new IngestionResult(
            source.type(),
            documents.size(),
            totalChunks,
            totalEmbeddings,
            "SUCCESS",
            logs,
            Instant.now()
        );
    }

    public int ingestSingleDocument(Document doc) {
        if (doc == null || doc.content() == null || doc.content().isBlank()) {
            return 0;
        }

        // Clean up stale chunks for this document before saving newly generated embeddings
        vectorStore.deleteChunksByDocumentId(doc.id());

        List<String> sections = chunkGenerator.splitIntoSections(doc);
        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (String sectionText : sections) {
            float[] chunkVector = embeddingGenerator.generateEmbedding(sectionText);
            int approxTokens = Math.max(1, sectionText.length() / 4);
            Chunk chunk = new Chunk(
                doc.id() + "-chunk-" + (++chunkIndex),
                doc.id(),
                doc.title(),
                doc.sourceType(),
                sectionText,
                chunkVector,
                approxTokens,
                doc.metadata(),
                0.0
            );
            chunks.add(chunk);
        }

        vectorStore.saveChunks(chunks);
        return chunks.size();
    }
}

