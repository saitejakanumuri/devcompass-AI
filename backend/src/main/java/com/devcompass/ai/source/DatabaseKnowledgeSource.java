package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SchemaMetadata;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.service.SchemaExplorerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseKnowledgeSource implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseKnowledgeSource.class);

    private final SchemaExplorerService schemaExplorerService;

    public DatabaseKnowledgeSource() {
        this(new SchemaExplorerService());
    }

    @Autowired
    public DatabaseKnowledgeSource(SchemaExplorerService schemaExplorerService) {
        this.schemaExplorerService = schemaExplorerService != null ? schemaExplorerService : new SchemaExplorerService();
    }

    public Optional<String> validateConfig() {
        if (schemaExplorerService == null) {
            return Optional.of("Database SchemaExplorerService is uninitialized.");
        }
        try {
            List<SchemaMetadata> schemas = schemaExplorerService.getAllSchemas();
            if (schemas == null || schemas.isEmpty()) {
                return Optional.of("Could not extract database table schemas. Verify target database connection URL and credentials.");
            }
        } catch (Exception e) {
            return Optional.of("Database validation error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Document> sync() {
        log.info("[DatabaseKnowledgeSource] Syncing database schema and table relationships...");
        List<SchemaMetadata> schemas = schemaExplorerService.getAllSchemas();
        return buildDocumentsFromSchemas(schemas);
    }

    /**
     * Build RAG documents from a pre-fetched, account-scoped list of schemas.
     * Called by AccountConfigController to avoid re-fetching schemas globally.
     */
    public List<Document> buildDocumentsFromSchemas(List<SchemaMetadata> schemas) {
        Map<String, List<ReverseRef>> reverseRefsMap = buildReverseReferencesMap(schemas);
        List<Document> documents = new ArrayList<>();

        for (SchemaMetadata schema : schemas) {
            List<ReverseRef> incomingRefs = reverseRefsMap.getOrDefault(schema.tableName().toLowerCase(), List.of());
            String markdownContent = buildSchemaMarkdown(schema, incomingRefs);
            String docId = "db-schema-" + schema.tableName();

            Document doc = new Document(
                UUID.randomUUID().toString(),
                "INFORMATION_SCHEMA - " + schema.tableName() + " Table & Foreign Keys",
                docId,
                SourceType.DATABASE_METADATA,
                markdownContent,
                Map.of(
                    "tableSchema", schema.tableSchema(),
                    "tableName", schema.tableName(),
                    "vectorIndexed", schema.vectorIndexed(),
                    "columnCount", schema.columns().size(),
                    "primaryKeyCount", schema.primaryKeys().size(),
                    "foreignKeyCount", schema.foreignKeys().size(),
                    "incomingReferenceCount", incomingRefs.size(),
                    "source", "Live ANSI INFORMATION_SCHEMA"
                )
            );
            documents.add(doc);
        }

        log.info("[DatabaseKnowledgeSource] Built {} database schema RAG documents.", documents.size());
        return documents;
    }

    private Map<String, List<ReverseRef>> buildReverseReferencesMap(List<SchemaMetadata> schemas) {
        Map<String, List<ReverseRef>> map = new HashMap<>();

        for (SchemaMetadata sourceSchema : schemas) {
            for (SchemaMetadata.ForeignKeyMetadata fk : sourceSchema.foreignKeys()) {
                String targetTable = fk.targetTable().toLowerCase();
                map.computeIfAbsent(targetTable, k -> new ArrayList<>()).add(
                    new ReverseRef(sourceSchema.tableName(), fk.columnName(), fk.targetColumn())
                );
            }
        }

        return map;
    }

    private String buildSchemaMarkdown(SchemaMetadata schema, List<ReverseRef> incomingRefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Database Table & Relationship Specification: ").append(schema.tableSchema()).append(".").append(schema.tableName()).append("\n\n");
        sb.append("## Table Description\n");
        sb.append(schema.description()).append("\n\n");

        sb.append("## Column Definitions\n");
        for (SchemaMetadata.ColumnMetadata col : schema.columns()) {
            boolean isPk = schema.primaryKeys().contains(col.name());
            sb.append("- `").append(col.name()).append("` (").append(col.dataType()).append(")");
            if (isPk) {
                sb.append(" [PRIMARY KEY]");
            }
            if (!col.nullable()) {
                sb.append(" NOT NULL");
            }
            sb.append(": ").append(col.description()).append("\n");
        }
        sb.append("\n");

        sb.append("## Primary Keys\n");
        if (schema.primaryKeys().isEmpty()) {
            sb.append("No explicit primary keys defined.\n\n");
        } else {
            sb.append("Primary key constraint columns: ").append(String.join(", ", schema.primaryKeys())).append("\n\n");
        }

        sb.append("## Outgoing Table Relationships (Foreign Keys)\n");
        if (schema.foreignKeys().isEmpty()) {
            sb.append("No outgoing foreign key constraints.\n\n");
        } else {
            for (SchemaMetadata.ForeignKeyMetadata fk : schema.foreignKeys()) {
                sb.append("- **Foreign Key Constraint**: Column `").append(fk.columnName())
                  .append("` references target table `").append(fk.targetTable())
                  .append("` on target column `").append(fk.targetColumn())
                  .append("`.\n");
                sb.append("  - **SQL Join Snippet**: `SELECT * FROM ").append(schema.tableName())
                  .append(" JOIN ").append(fk.targetTable())
                  .append(" ON ").append(schema.tableName()).append(".").append(fk.columnName())
                  .append(" = ").append(fk.targetTable()).append(".").append(fk.targetColumn()).append("`\n");
            }
            sb.append("\n");
        }

        sb.append("## Incoming Table Relationships (Referenced By)\n");
        if (incomingRefs.isEmpty()) {
            sb.append("No other tables reference this table as a foreign key target.\n\n");
        } else {
            for (ReverseRef ref : incomingRefs) {
                sb.append("- **Referenced By Table**: `").append(ref.childTable())
                  .append("` on column `").append(ref.childColumn())
                  .append("` referencing `").append(schema.tableName()).append(".").append(ref.parentColumn())
                  .append("`.\n");
                sb.append("  - **SQL Reverse Join Snippet**: `SELECT * FROM ").append(schema.tableName())
                  .append(" JOIN ").append(ref.childTable())
                  .append(" ON ").append(schema.tableName()).append(".").append(ref.parentColumn())
                  .append(" = ").append(ref.childTable()).append(".").append(ref.childColumn()).append("`\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private record ReverseRef(String childTable, String childColumn, String parentColumn) {}

    @Override
    public SourceType type() {
        return SourceType.DATABASE_METADATA;
    }

    @Override
    public String sourceName() {
        return "Amazon RDS PostgreSQL INFORMATION_SCHEMA Metadata";
    }

    @Override
    public boolean isHealthy() {
        return validateConfig().isEmpty();
    }
}
