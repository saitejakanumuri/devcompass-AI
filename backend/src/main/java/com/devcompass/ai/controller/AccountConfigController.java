package com.devcompass.ai.controller;

import com.devcompass.ai.model.AccountKnowledgeConfig;
import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.repository.AccountConfigRepository;
import com.devcompass.ai.security.AuthenticatedUser;
import com.devcompass.ai.source.DatabaseKnowledgeSource;
import com.devcompass.ai.source.GitKnowledgeSource;
import com.devcompass.ai.source.NotionKnowledgeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Manages the user's knowledge source configurations (Notion, Git, Database).
 * Each user can save config, trigger sync, and view their sources.
 */
@RestController
@RequestMapping("/api/v1/sources")
public class AccountConfigController {

    private static final Logger log = LoggerFactory.getLogger(AccountConfigController.class);

    private final AccountConfigRepository configRepository;
    private final IngestionPipelineService ingestionPipeline;
    private final GitKnowledgeSource gitSource;
    private final NotionKnowledgeSource notionSource;
    private final DatabaseKnowledgeSource dbSource;

    public AccountConfigController(
        AccountConfigRepository configRepository,
        IngestionPipelineService ingestionPipeline,
        GitKnowledgeSource gitSource,
        NotionKnowledgeSource notionSource,
        DatabaseKnowledgeSource dbSource
    ) {
        this.configRepository = configRepository;
        this.ingestionPipeline = ingestionPipeline;
        this.gitSource = gitSource;
        this.notionSource = notionSource;
        this.dbSource = dbSource;
    }

    /** GET /api/v1/sources — list this user's configured knowledge sources */
    @GetMapping
    public ResponseEntity<List<AccountKnowledgeConfig>> getConfigs(
        @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        UUID userId = principal.userId();
        List<AccountKnowledgeConfig> configs = configRepository.getConfigsForUser(userId);
        return ResponseEntity.ok(configs);
    }

    /** POST /api/v1/sources/{sourceType} — save or update a knowledge source config */
    @PostMapping("/{sourceType}")
    public ResponseEntity<?> saveConfig(
        @PathVariable String sourceType,
        @RequestBody Map<String, Object> configJson,
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam(defaultValue = "false") boolean autoSync
    ) {
        UUID userId = principal.userId();
        SourceType type;
        try {
            type = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid source type: " + sourceType));
        }

        AccountKnowledgeConfig saved = configRepository.saveOrUpdateConfig(userId, type, configJson, "CONFIGURED");

        IngestionResult syncResult = null;
        if (autoSync) {
            syncResult = doSync(userId, type);
            configRepository.updateLastSyncedAt(userId, type, syncResult.status());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("config", saved);
        if (syncResult != null) response.put("syncResult", syncResult);

        log.info("[Sources] Saved {} config for userId={}", type, userId);
        return ResponseEntity.ok(response);
    }

    /** POST /api/v1/sources/{sourceType}/sync — trigger sync for a knowledge source */
    @PostMapping("/{sourceType}/sync")
    public ResponseEntity<?> syncSource(
        @PathVariable String sourceType,
        @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        UUID userId = principal.userId();
        SourceType type;
        try {
            type = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid source type: " + sourceType));
        }

        Optional<AccountKnowledgeConfig> configOpt = configRepository.getConfigForUserAndType(userId, type);
        if (configOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "No configuration found for " + type + ". Save a configuration first."
            ));
        }

        log.info("[Sources] Sync triggered: type={} userId={}", type, userId);
        IngestionResult result = doSync(userId, type);
        configRepository.updateLastSyncedAt(userId, type, result.status());
        return ResponseEntity.ok(result);
    }

    /** Internal: fetch documents from source using user's saved config, then ingest */
    private IngestionResult doSync(UUID userId, SourceType type) {
        try {
            List<Document> documents = fetchDocuments(userId, type);
            if (documents.isEmpty()) {
                return new IngestionResult(type, 0, 0, 0, "NO_DATA",
                    List.of("No documents found. Check your configuration."), Instant.now());
            }
            return ingestionPipeline.ingestDocumentsForAccount(userId, type, documents);
        } catch (Exception e) {
            log.error("[Sources] Sync failed: userId={} type={}: {}", userId, type, e.getMessage());
            return new IngestionResult(type, 0, 0, 0, "FAILED",
                List.of("Sync failed: " + e.getMessage()), Instant.now());
        }
    }

    private List<Document> fetchDocuments(UUID userId, SourceType type) {
        Optional<AccountKnowledgeConfig> configOpt = configRepository.getConfigForUserAndType(userId, type);
        if (configOpt.isEmpty()) return List.of();

        Map<String, Object> cfg = configOpt.get().configJson();

        return switch (type) {
            case GIT_REPOSITORY -> gitSource.sync(cfg);
            case NOTION -> notionSource.sync(cfg);
            case DATABASE_METADATA -> dbSource.sync(cfg);
            default -> List.of();
        };
    }

    private String str(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString() : defaultVal;
    }
}
