package com.devcompass.ai.controller;

import com.devcompass.ai.embedding.EmbeddingProviderFactory;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.pipeline.VectorStore;
import com.devcompass.ai.provider.AIProviderFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineController {

    private final IngestionPipelineService ingestionPipelineService;
    private final VectorStore vectorStore;
    private final EmbeddingProviderFactory embeddingProviderFactory;
    private final AIProviderFactory aiProviderFactory;

    public PipelineController(
        IngestionPipelineService ingestionPipelineService,
        VectorStore vectorStore,
        EmbeddingProviderFactory embeddingProviderFactory,
        AIProviderFactory aiProviderFactory
    ) {
        this.ingestionPipelineService = ingestionPipelineService;
        this.vectorStore = vectorStore;
        this.embeddingProviderFactory = embeddingProviderFactory;
        this.aiProviderFactory = aiProviderFactory;
    }

    @PostMapping("/ingest")
    public ResponseEntity<List<IngestionResult>> runFullIngestion() {
        List<IngestionResult> results = ingestionPipelineService.runFullIngestionPipeline();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPipelineStatus() {
        return ResponseEntity.ok(Map.of(
            "totalChunksIndexed", vectorStore.totalIndexedChunks(),
            "pipelineStatus", "ACTIVE",
            "activeEmbeddingProvider", embeddingProviderFactory.getActiveProvider().providerName(),
            "activeVectorDatabase", vectorStore.storeName(),
            "rdsInitialized", vectorStore.isInitialized(),
            "activeLLMAnswerGenerator", aiProviderFactory.getActiveProvider().providerName() + " (" + aiProviderFactory.getActiveProvider().modelName() + ")",
            "dimension", embeddingProviderFactory.getActiveProvider().dimension()
        ));
    }

    @GetMapping("/architecture")
    public ResponseEntity<Map<String, Object>> getProductionArchitecture() {
        return ResponseEntity.ok(Map.of(
            "embeddingsEngine", Map.of(
                "provider", "Ollama",
                "model", "nomic-embed-text",
                "endpoint", "http://localhost:11434/api/embeddings",
                "dimension", 768,
                "status", "ONLINE_LOCAL"
            ),
            "vectorDatabase", Map.of(
                "database", vectorStore.storeName(),
                "tableName", vectorStore.tableName(),
                "distanceMetric", "Cosine Distance (<=>)",
                "rdsConnected", vectorStore.isInitialized(),
                "sampleQuery", """
                    SELECT id, document_title, source_type, content, 
                           1 - (embedding <=> '[0.012,-0.045,...]'::vector) AS cosine_similarity
                    FROM %s
                    WHERE 1 - (embedding <=> '[0.012,-0.045,...]'::vector) >= 0.70
                    ORDER BY embedding <=> '[0.012,-0.045,...]'::vector ASC
                    LIMIT 5;
                    """.formatted(vectorStore.tableName())
            ),
            "llmAnswerGenerator", Map.of(
                "provider", "Google Gemini",
                "model", "gemini-1.5-pro",
                "contextWindow", "1,000,000 tokens",
                "status", "READY"
            )
        ));
    }
}
