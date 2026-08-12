package com.devcompass.ai.controller;

import com.devcompass.ai.model.AccountKnowledgeConfig;
import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SchemaMetadata;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.repository.AccountConfigRepository;
import com.devcompass.ai.service.SchemaExplorerService;
import com.devcompass.ai.source.DatabaseKnowledgeSource;
import com.devcompass.ai.source.GitKnowledgeSource;
import com.devcompass.ai.source.NotionKnowledgeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/account")
public class AccountConfigController {

    private static final Logger log = LoggerFactory.getLogger(AccountConfigController.class);

    private final AccountConfigRepository accountConfigRepository;
    private final IngestionPipelineService ingestionPipelineService;
    private final GitKnowledgeSource gitKnowledgeSource;
    private final NotionKnowledgeSource notionKnowledgeSource;
    private final DatabaseKnowledgeSource databaseKnowledgeSource;
    private final SchemaExplorerService schemaExplorerService;

    @Autowired
    public AccountConfigController(
        AccountConfigRepository accountConfigRepository,
        IngestionPipelineService ingestionPipelineService,
        GitKnowledgeSource gitKnowledgeSource,
        NotionKnowledgeSource notionKnowledgeSource,
        DatabaseKnowledgeSource databaseKnowledgeSource,
        SchemaExplorerService schemaExplorerService
    ) {
        this.accountConfigRepository = accountConfigRepository;
        this.ingestionPipelineService = ingestionPipelineService;
        this.gitKnowledgeSource = gitKnowledgeSource;
        this.notionKnowledgeSource = notionKnowledgeSource;
        this.databaseKnowledgeSource = databaseKnowledgeSource;
        this.schemaExplorerService = schemaExplorerService;
    }

    /**
     * GET /api/v1/account/configs
     * Fetches only this account's configured knowledge sources.
     */
    @GetMapping("/configs")
    public ResponseEntity<List<AccountKnowledgeConfig>> getAccountConfigs(
        @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
        @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        UUID accountId = resolveAccountId(headerAccountId);
        UUID userId = resolveUUID(headerUserId);
        // Always fetch only this account's configs — never global
        List<AccountKnowledgeConfig> configs = accountConfigRepository.getEffectiveConfigsForAccountAndUser(accountId, userId);
        return ResponseEntity.ok(configs);
    }

    /**
     * POST /api/v1/account/configs/{sourceType}
     * Save configuration for a knowledge source under this account.
     * The config is stored with the account_id so it is always account-scoped.
     */
    @PostMapping("/configs/{sourceType}")
    public ResponseEntity<?> saveAccountConfig(
        @PathVariable String sourceType,
        @RequestBody Map<String, Object> configJson,
        @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
        @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
        @RequestParam(defaultValue = "false") boolean autoSync,
        @RequestParam(defaultValue = "false") boolean isUserOverride
    ) {
        UUID accountId = resolveAccountId(headerAccountId);
        UUID userId = isUserOverride ? resolveUUID(headerUserId) : null;
        SourceType type;
        try {
            type = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid source type: " + sourceType));
        }

        // Persist config to DB under this account_id — never global
        AccountKnowledgeConfig savedConfig = accountConfigRepository.saveOrUpdateUserConfig(
            accountId, userId, type, configJson, "CONFIGURED"
        );

        // Optional auto-sync: only sync this account's data
        IngestionResult syncResult = null;
        if (autoSync) {
            syncResult = syncAccountSource(accountId, userId, type);
            accountConfigRepository.updateLastSyncedAt(accountId, type, syncResult.status());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("config", savedConfig);
        if (syncResult != null) {
            response.put("syncResult", syncResult);
        }

        log.info("[AccountConfigController] Saved {} config for accountId={} userOverride={}", type, accountId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/account/sync/{sourceType}
     * Manually trigger sync for ONE source type scoped to the current account only.
     * Never syncs all accounts or unrelated sources.
     */
    @PostMapping("/sync/{sourceType}")
    public ResponseEntity<?> syncIndividualKnowledgeSource(
        @PathVariable String sourceType,
        @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
        @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        UUID accountId = resolveAccountId(headerAccountId);
        UUID userId = resolveUUID(headerUserId);
        SourceType type;
        try {
            type = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid source type: " + sourceType));
        }

        // Ensure this account has a config for this source type before attempting sync
        Optional<AccountKnowledgeConfig> configOpt = accountConfigRepository.getConfigForAccountUserAndType(accountId, userId, type);
        if (configOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "sourceType", type.name(),
                "message", "No configuration found for this source type under your account. Please save a configuration first."
            ));
        }

        log.info("[AccountConfigController] Account-scoped sync triggered for type={} accountId={}", type, accountId);
        IngestionResult result = syncAccountSource(accountId, userId, type);
        accountConfigRepository.updateLastSyncedAt(accountId, type, result.status());

        return ResponseEntity.ok(result);
    }

    /**
     * Core account-scoped sync logic.
     * Loads this account's saved config, applies it to the source, fetches documents
     * only for this account, and indexes them without touching other accounts' data.
     */
    private IngestionResult syncAccountSource(UUID accountId, UUID userId, SourceType type) {
        try {
            List<Document> documents = fetchDocumentsForAccount(accountId, userId, type);
            if (documents.isEmpty()) {
                log.warn("[AccountConfigController] No documents produced for accountId={} type={}", accountId, type);
                return new IngestionResult(type, 0, 0, 0, "NO_DATA",
                    List.of("No documents found. Check your configuration credentials."),
                    java.time.Instant.now());
            }
            return ingestionPipelineService.ingestDocumentsForAccount(accountId, type, documents);
        } catch (Exception e) {
            log.error("[AccountConfigController] Sync failed for accountId={} type={}: {}", accountId, type, e.getMessage(), e);
            return new IngestionResult(type, 0, 0, 0, "FAILED",
                List.of("Sync failed: " + e.getMessage()),
                java.time.Instant.now());
        }
    }

    /**
     * Fetch source documents scoped to this account's configuration.
     * Each source type uses this account's saved credentials — never global defaults.
     */
    private List<Document> fetchDocumentsForAccount(UUID accountId, UUID userId, SourceType type) {
        Optional<AccountKnowledgeConfig> configOpt = accountConfigRepository.getConfigForAccountUserAndType(accountId, userId, type);
        if (configOpt.isEmpty()) return List.of();

        Map<String, Object> cfg = configOpt.get().configJson();

        return switch (type) {
            case DATABASE_METADATA -> {
                // Use account's DB credentials via SchemaExplorerService to get account-specific schemas
                List<SchemaMetadata> schemas = schemaExplorerService.getSchemasForAccount(accountId, userId);
                yield databaseKnowledgeSource.buildDocumentsFromSchemas(schemas);
            }
            case GIT_REPOSITORY -> {
                String repoUrl = cfg.getOrDefault("repoUrl", "").toString();
                String repoPath = cfg.getOrDefault("repoPath", "./").toString();
                String branch = cfg.getOrDefault("branch", "main").toString();
                String exts = cfg.getOrDefault("includedExtensions", "java,ts,tsx,py,go,rs,yml,yaml,md,json").toString();
                Long maxSize = 500L;
                try { maxSize = Long.parseLong(cfg.getOrDefault("maxFileSizeKb", "500").toString()); } catch (Exception ignored) {}
                gitKnowledgeSource.updateConfig(repoUrl, repoPath, branch, exts, maxSize);
                yield gitKnowledgeSource.sync();
            }
            case NOTION -> {
                String token = cfg.getOrDefault("apiToken", cfg.getOrDefault("apiKey", cfg.getOrDefault("token", ""))).toString();
                String pageId = cfg.getOrDefault("mainPageId", cfg.getOrDefault("databaseId", "")).toString();
                notionKnowledgeSource.updateConfig(token, pageId);
                yield notionKnowledgeSource.sync();
            }
            default -> List.of();
        };
    }

    private Optional<String> validateSourceType(SourceType type) {
        if (type == SourceType.GIT_REPOSITORY) {
            return gitKnowledgeSource.validateConfig();
        } else if (type == SourceType.DATABASE_METADATA) {
            return databaseKnowledgeSource.validateConfig();
        } else if (type == SourceType.NOTION) {
            return notionKnowledgeSource.validateConfig();
        }
        return Optional.empty();
    }

    private UUID resolveAccountId(String headerAccountId) {
        if (headerAccountId != null && !headerAccountId.isBlank()) {
            try {
                return UUID.fromString(headerAccountId.trim());
            } catch (Exception ignored) {}
        }
        return UUID.nameUUIDFromBytes("default-demo-account".getBytes());
    }

    private UUID resolveUUID(String val) {
        if (val != null && !val.isBlank()) {
            try {
                return UUID.fromString(val.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }
}
