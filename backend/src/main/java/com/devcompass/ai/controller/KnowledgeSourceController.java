package com.devcompass.ai.controller;

import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.source.KnowledgeSource;
import com.devcompass.ai.source.KnowledgeSourceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sources")
public class KnowledgeSourceController {

    private final KnowledgeSourceRegistry sourceRegistry;
    private final IngestionPipelineService ingestionPipelineService;

    public KnowledgeSourceController(KnowledgeSourceRegistry sourceRegistry, IngestionPipelineService ingestionPipelineService) {
        this.sourceRegistry = sourceRegistry;
        this.ingestionPipelineService = ingestionPipelineService;
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
    public ResponseEntity<IngestionResult> syncSource(@PathVariable String sourceType) {
        try {
            SourceType type = SourceType.valueOf(sourceType.toUpperCase());
            IngestionResult result = ingestionPipelineService.ingestSourceType(type);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
