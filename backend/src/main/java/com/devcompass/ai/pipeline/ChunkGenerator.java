package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChunkGenerator {

    private final int defaultChunkSize;    // Target max tokens per chunk (e.g. 512 tokens)
    private final int defaultChunkOverlap; // Overlapping tokens between consecutive chunks (e.g. 64 tokens)

    public ChunkGenerator(
        @Value("${devcompass.ingestion.chunk-size:512}") int chunkSize,
        @Value("${devcompass.ingestion.chunk-overlap:64}") int chunkOverlap
    ) {
        this.defaultChunkSize = chunkSize;
        this.defaultChunkOverlap = chunkOverlap;
    }

    /**
     * Production Sliding-Window Chunker that uses defaultChunkSize and defaultChunkOverlap
     * to prevent context loss at chunk boundaries and optimize vector retrieval accuracy.
     */
    public List<String> splitIntoSections(Document document) {
        String text = document.content();
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Approximate token-to-character ratio (1 token ~ 4 characters)
        int maxChunkChars = defaultChunkSize * 4;
        int overlapChars = defaultChunkOverlap * 4;

        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + maxChunkChars, textLength);

            // Break at nearest paragraph (\n\n), line break (\n), or sentence boundary (. ) to preserve semantic structure
            if (end < textLength) {
                int lastParagraph = text.lastIndexOf("\n\n", end);
                int lastNewline = text.lastIndexOf("\n", end);
                int lastPeriod = text.lastIndexOf(". ", end);

                if (lastParagraph > start + overlapChars) {
                    end = lastParagraph + 2;
                } else if (lastNewline > start + overlapChars) {
                    end = lastNewline + 1;
                } else if (lastPeriod > start + overlapChars) {
                    end = lastPeriod + 2;
                }
            }

            String chunkContent = text.substring(start, end).trim();
            if (!chunkContent.isBlank()) {
                chunks.add(chunkContent);
            }

            if (end >= textLength) {
                break;
            }

            // Move sliding window start position back by overlapChars to maintain cross-boundary context
            start = Math.max(start + 1, end - overlapChars);
        }

        return chunks;
    }

    /**
     * Legacy helper method for chunk creation.
     */
    public List<Chunk> generateChunks(Document document, float[] embedding) {
        List<String> sections = splitIntoSections(document);
        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (String section : sections) {
            int approxTokens = Math.max(1, section.length() / 4);
            Chunk chunk = new Chunk(
                document.id() + "-chunk-" + (++chunkIndex),
                document.id(),
                document.title(),
                document.sourceType(),
                section,
                embedding,
                approxTokens,
                document.metadata(),
                0.0
            );
            chunks.add(chunk);
        }

        return chunks;
    }

    public int getDefaultChunkSize() {
        return defaultChunkSize;
    }

    public int getDefaultChunkOverlap() {
        return defaultChunkOverlap;
    }
}
