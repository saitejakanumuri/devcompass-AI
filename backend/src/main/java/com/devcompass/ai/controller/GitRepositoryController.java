package com.devcompass.ai.controller;

import com.devcompass.ai.model.GitRepoConfigRequest;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.source.GitKnowledgeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/git")
public class GitRepositoryController {

    private final GitKnowledgeSource gitKnowledgeSource;
    private final IngestionPipelineService ingestionPipelineService;

    @Autowired
    public GitRepositoryController(
        GitKnowledgeSource gitKnowledgeSource,
        IngestionPipelineService ingestionPipelineService
    ) {
        this.gitKnowledgeSource = gitKnowledgeSource;
        this.ingestionPipelineService = ingestionPipelineService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getGitConfig() {
        return ResponseEntity.ok(gitKnowledgeSource.getConfig());
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateGitConfig(
        @RequestBody GitRepoConfigRequest request,
        @RequestParam(defaultValue = "false") boolean autoSync
    ) {
        gitKnowledgeSource.updateConfig(
            request.repoUrl(),
            request.repoPath(),
            request.branch(),
            request.includedExtensions(),
            request.maxFileSizeKb()
        );

        Optional<String> validationErr = gitKnowledgeSource.validateConfig();
        if (validationErr.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Configuration validation failed: " + validationErr.get(),
                "config", gitKnowledgeSource.getConfig()
            ));
        }

        IngestionResult syncResult = null;
        if (autoSync) {
            syncResult = ingestionPipelineService.ingestSourceType(SourceType.GIT_REPOSITORY);
        }

        Map<String, Object> response = new HashMap<>(gitKnowledgeSource.getConfig());
        response.put("status", "SUCCESS");
        if (syncResult != null) {
            response.put("syncResult", syncResult);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncGitRepository() {
        Optional<String> validationErr = gitKnowledgeSource.validateConfig();
        if (validationErr.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Cannot sync Git repository. " + validationErr.get(),
                "config", gitKnowledgeSource.getConfig()
            ));
        }

        IngestionResult result = ingestionPipelineService.ingestSourceType(SourceType.GIT_REPOSITORY);
        return ResponseEntity.ok(result);
    }
}
