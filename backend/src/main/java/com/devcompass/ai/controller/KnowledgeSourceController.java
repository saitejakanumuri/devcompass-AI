package com.devcompass.ai.controller;

import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.source.DatabaseKnowledgeSource;
import com.devcompass.ai.source.GitKnowledgeSource;
import com.devcompass.ai.source.KnowledgeSource;
import com.devcompass.ai.source.KnowledgeSourceRegistry;
import com.devcompass.ai.source.NotionKnowledgeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sources")
public class KnowledgeSourceController {

    private final KnowledgeSourceRegistry sourceRegistry;
    private final IngestionPipelineService ingestionPipelineService;
    private final NotionKnowledgeSource notionKnowledgeSource;
    private final GitKnowledgeSource gitKnowledgeSource;
    private final DatabaseKnowledgeSource databaseKnowledgeSource;

    @Autowired
    public KnowledgeSourceController(
        KnowledgeSourceRegistry sourceRegistry,
        IngestionPipelineService ingestionPipelineService,
        NotionKnowledgeSource notionKnowledgeSource,
        GitKnowledgeSource gitKnowledgeSource,
        DatabaseKnowledgeSource databaseKnowledgeSource
    ) {
        this.sourceRegistry = sourceRegistry;
        this.ingestionPipelineService = ingestionPipelineService;
        this.notionKnowledgeSource = notionKnowledgeSource;
        this.gitKnowledgeSource = gitKnowledgeSource;
        this.databaseKnowledgeSource = databaseKnowledgeSource;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listKnowledgeSources() {
        List<Map<String, Object>> response = sourceRegistry.getAllSources().stream()
            .map(s -> Map.<String, Object>of(
                "type", s.type().name(),
                "name", s.sourceName(),
                "healthy", s.isHealthy()
            ))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync/{sourceType}")
    public ResponseEntity<?> syncSource(@PathVariable String sourceType) {
        try {
            SourceType type = SourceType.valueOf(sourceType.toUpperCase());

            // Explicit pre-sync validation for each source type
            Optional<String> validationErr = Optional.empty();
            switch (type) {
                case GIT_REPOSITORY -> validationErr = gitKnowledgeSource.validateConfig();
                case DATABASE_METADATA -> validationErr = databaseKnowledgeSource.validateConfig();
                case NOTION -> validationErr = notionKnowledgeSource.validateConfig();
            }

            if (validationErr.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "sourceType", type.name(),
                    "message", "Cannot sync knowledge source. " + validationErr.get()
                ));
            }

            IngestionResult result = ingestionPipelineService.ingestSourceType(type);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Unknown or unsupported knowledge source type: " + sourceType
            ));
        }
    }
}
