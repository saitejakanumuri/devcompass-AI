package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChunkGeneratorTest {

    @Test
    void testSlidingWindowChunkingWithOverlap() {
        ChunkGenerator chunker = new ChunkGenerator(50, 10); // 50 tokens (~200 chars), 10 tokens (~40 chars overlap)
        assertEquals(50, chunker.getDefaultChunkSize());
        assertEquals(10, chunker.getDefaultChunkOverlap());

        String longDocContent = """
            Paragraph 1: The SEO Application governs canonical URL generation, open graph social cards, dynamic sitemaps, and search engine microdata across all tutor profile pages.
            
            Paragraph 2: Tutor Profile & Onboarding Service is owned by the Acquisition Team. Database migrations are managed via Liquibase and Flyway scripts.
            
            Paragraph 3: CI/CD deployment pipelines run automated GitHub Actions steps building Docker containers and pushing to AWS ECR. ArgoCD synchronizes Helm charts to Kubernetes EKS clusters.
            """;

        Document doc = new Document("doc-101", "Architecture Spec", "notion-101", SourceType.NOTION, longDocContent, Map.of());
        List<String> chunks = chunker.splitIntoSections(doc);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
    }
}
