package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeSourceTest {

    @Test
    void testNotionSourceExtraction() {
        NotionKnowledgeSource notion = new NotionKnowledgeSource();
        assertEquals(SourceType.NOTION, notion.type());
        assertTrue(notion.isHealthy());

        List<Document> docs = notion.sync();
        assertFalse(docs.isEmpty());
        assertTrue(docs.get(0).title().contains("SEO"));
    }

    @Test
    void testGitSourceExtraction() {
        GitKnowledgeSource git = new GitKnowledgeSource();
        assertEquals(SourceType.GIT_REPOSITORY, git.type());

        List<Document> docs = git.sync();
        assertFalse(docs.isEmpty());
        assertTrue(docs.get(0).content().contains("package"));
    }
}
