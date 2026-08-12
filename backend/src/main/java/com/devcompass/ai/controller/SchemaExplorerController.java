package com.devcompass.ai.controller;

import com.devcompass.ai.model.DatabaseConnectionRequest;
import com.devcompass.ai.model.IngestionResult;
import com.devcompass.ai.model.SchemaMetadata;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.service.SchemaExplorerService;
import com.devcompass.ai.source.DatabaseKnowledgeSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schema")
public class SchemaExplorerController {

    private final SchemaExplorerService schemaExplorerService;
    private final DatabaseKnowledgeSource databaseKnowledgeSource;
    private final IngestionPipelineService ingestionPipelineService;

    @Autowired
    public SchemaExplorerController(
        SchemaExplorerService schemaExplorerService,
        DatabaseKnowledgeSource databaseKnowledgeSource,
        IngestionPipelineService ingestionPipelineService
    ) {
        this.schemaExplorerService = schemaExplorerService;
        this.databaseKnowledgeSource = databaseKnowledgeSource;
        this.ingestionPipelineService = ingestionPipelineService;
    }

    @GetMapping("/tables")
    public ResponseEntity<List<SchemaMetadata>> getAllTableSchemas(
        @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
        @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        UUID accountId = resolveUUID(headerAccountId);
        UUID userId = resolveUUID(headerUserId);
        return ResponseEntity.ok(schemaExplorerService.getSchemasForAccount(accountId, userId));
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<SchemaMetadata> getTableSchema(
        @PathVariable String tableName,
        @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
        @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        UUID accountId = resolveUUID(headerAccountId);
        UUID userId = resolveUUID(headerUserId);
        return schemaExplorerService.getSchemaForTable(tableName, accountId, userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    private UUID resolveUUID(String val) {
        if (val != null && !val.isBlank()) {
            try {
                return UUID.fromString(val.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection(@RequestBody DatabaseConnectionRequest request) {
        boolean connected = schemaExplorerService.testConnection(
            request.url(),
            request.username(),
            request.password()
        );

        if (connected) {
            return ResponseEntity.ok(Map.of(
                "status", "CONNECTED",
                "message", "Successfully established connection to target database and verified ANSI INFORMATION_SCHEMA access.",
                "url", request.url() != null ? request.url() : ""
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
            "status", "FAILED",
            "message", "Could not connect to target database with provided credentials. Verify URL, username, and password.",
            "url", request.url() != null ? request.url() : ""
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncDatabaseSchemaRelationships() {
        Optional<String> validationErr = databaseKnowledgeSource.validateConfig();
        if (validationErr.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Cannot sync database schema. " + validationErr.get()
            ));
        }

        IngestionResult result = ingestionPipelineService.ingestSourceType(SourceType.DATABASE_METADATA);
        return ResponseEntity.ok(result);
    }
}
