package com.devcompass.ai.service;

import com.devcompass.ai.model.*;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.pipeline.VectorStore;
import com.devcompass.ai.provider.AIProvider;
import com.devcompass.ai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QueryEngineService {

    private static final Logger log = LoggerFactory.getLogger(QueryEngineService.class);

    private final VectorStore vectorStore;
    private final AIProviderFactory providerFactory;
    private final IngestionPipelineService ingestionPipelineService;

    public QueryEngineService(
        VectorStore vectorStore,
        AIProviderFactory providerFactory,
        IngestionPipelineService ingestionPipelineService
    ) {
        this.vectorStore = vectorStore;
        this.providerFactory = providerFactory;
        this.ingestionPipelineService = ingestionPipelineService;
    }

    public static final double MIN_RELEVANCE_THRESHOLD = 0.30;

    public QueryResponse executeQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("[QueryEngineService] Processing query: '{}', Requested Provider: '{}'", request.question(), request.provider());

        // Ensure store has data if empty
        int totalIndexed = vectorStore.totalIndexedChunks();
        log.info("[QueryEngineService] Current total indexed chunks in vector store: {}", totalIndexed);

        if (totalIndexed == 0) {
            log.info("[QueryEngineService] Vector store is empty. Triggering automated full ingestion pipeline...");
            ingestionPipelineService.runFullIngestionPipeline();
            log.info("[QueryEngineService] Ingestion complete. Total indexed chunks now: {}", vectorStore.totalIndexedChunks());
        }

        int topK = request.topK() != null ? request.topK() : 5;

        // 1. Semantic Retrieval (Enforcing minimum 30.0% relevance score threshold)
        List<Chunk> retrievedChunks = vectorStore.similaritySearch(
            request.question(),
            topK,
            MIN_RELEVANCE_THRESHOLD
        );

        log.info("[QueryEngineService] Retrieved {} chunks from vector store for query '{}'", retrievedChunks.size(), request.question());

        // 2. Build Prompt Context
        StringBuilder contextBuilder = new StringBuilder();
        List<Citation> citations = new ArrayList<>();

        for (Chunk chunk : retrievedChunks) {
            contextBuilder.append("--- SOURCE: ").append(chunk.documentTitle())
                .append(" (").append(chunk.sourceType()).append(") ---\n")
                .append(chunk.content()).append("\n\n");

            citations.add(new Citation(
                UUID.randomUUID().toString(),
                chunk.documentTitle(),
                chunk.sourceType(),
                chunk.documentId(),
                chunk.content().length() > 180 ? chunk.content().substring(0, 180) + "..." : chunk.content(),
                chunk.score(),
                chunk.metadata()
            ));
        }

        String fullContext = contextBuilder.toString();
        log.info("[QueryEngineService] Constructed RAG Context passed to LLM ({} chars):\n{}", fullContext.length(), fullContext.isBlank() ? "<EMPTY_CONTEXT>" : fullContext);

        // 3. System Prompt Construction
        String systemPrompt = request.systemPromptOverride() != null ? request.systemPromptOverride() : """
            You are DevCompass AI, an authoritative engineering assistant for developers.
            Answer the user's natural language question using ONLY the provided engineering context.
            If context is insufficient, state what is missing clearly. Do NOT guess external information.
            Include clear section headings and explicit code or table references.
            """;

        // 4. Provider Inference
        AIProvider provider = providerFactory.getProvider(request.provider());
        String answer = provider.generateAnswer(
            request.question(),
            fullContext,
            systemPrompt,
            request.temperature() != null ? request.temperature() : 0.2
        );

        long latency = System.currentTimeMillis() - startTime;
        int estimatedPromptTokens = (fullContext.length() + systemPrompt.length()) / 4;
        int estimatedCompletionTokens = answer.length() / 4;

        return new QueryResponse(
            request.question(),
            answer,
            provider.providerName().toUpperCase() + " (" + provider.modelName() + ")",
            citations,
            retrievedChunks,
            latency,
            estimatedPromptTokens,
            estimatedCompletionTokens
        );
    }
}
