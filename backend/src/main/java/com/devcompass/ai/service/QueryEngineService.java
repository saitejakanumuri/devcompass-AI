package com.devcompass.ai.service;

import com.devcompass.ai.model.*;
import com.devcompass.ai.pipeline.VectorStore;
import com.devcompass.ai.provider.AIProvider;
import com.devcompass.ai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core RAG query engine: similarity search on embeddings → build context → LLM answer.
 */
@Service
public class QueryEngineService {

    private static final Logger log = LoggerFactory.getLogger(QueryEngineService.class);
    private static final double MIN_RELEVANCE_THRESHOLD = 0.30;

    private final VectorStore vectorStore;
    private final AIProviderFactory providerFactory;

    public QueryEngineService(VectorStore vectorStore, AIProviderFactory providerFactory) {
        this.vectorStore = vectorStore;
        this.providerFactory = providerFactory;
    }

    public QueryResponse executeQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("[QueryEngine] Processing: '{}' provider='{}'", request.question(), request.provider());

        int topK = request.topK() != null ? request.topK() : 5;

        // 1. Similarity search on indexed embeddings
        List<Chunk> retrievedChunks = vectorStore.similaritySearch(
            request.question(), topK, MIN_RELEVANCE_THRESHOLD
        );
        log.info("[QueryEngine] Retrieved {} chunks", retrievedChunks.size());

        // 2. Build context from retrieved chunks
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

        String context = contextBuilder.toString();

        // 3. Build system prompt
        String systemPrompt = request.systemPromptOverride() != null ? request.systemPromptOverride() :
            """
            You are DevCompass AI, a helpful engineering assistant.
            Answer the user's question using ONLY the provided context.
            If the context is insufficient, say so clearly. Do not guess.
            """;

        // 4. Call LLM
        AIProvider provider = providerFactory.getProvider(request.provider());
        String answer = provider.generateAnswer(
            request.question(), context, systemPrompt,
            request.temperature() != null ? request.temperature() : 0.2
        );

        long latency = System.currentTimeMillis() - startTime;

        return new QueryResponse(
            request.question(),
            answer,
            provider.providerName() + " (" + provider.modelName() + ")",
            citations,
            retrievedChunks,
            latency,
            (context.length() + systemPrompt.length()) / 4,
            answer.length() / 4
        );
    }
}
