package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DatabaseKnowledgeSource implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseKnowledgeSource.class);



    @Override
    public List<Document> sync(Map<String, Object> config) {
        String jdbcUrl = str(config, "url", str(config, "jdbcUrl", ""));
        String username = str(config, "username", "");
        String password = str(config, "password", "");

        if (jdbcUrl.isBlank() || username.isBlank() || password.isBlank()) {
            log.warn("[DatabaseKnowledgeSource] Missing DB credentials. Cannot sync.");
            return List.of();
        }

        List<Document> documents = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            try (ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    StringBuilder content = new StringBuilder("Table: " + tableName + "\nColumns:\n");

                    try (ResultSet columns = metaData.getColumns(null, "public", tableName, "%")) {
                        while (columns.next()) {
                            content.append("- ").append(columns.getString("COLUMN_NAME"))
                                   .append(" (").append(columns.getString("TYPE_NAME")).append(")\n");
                        }
                    }

                    documents.add(new Document(
                        UUID.randomUUID().toString(),
                        "DB Schema: " + tableName,
                        "db-schema-" + tableName,
                        SourceType.DATABASE_METADATA,
                        content.toString(),
                        Map.of("tableName", tableName)
                    ));
                }
            }
        } catch (Exception e) {
            log.error("[DatabaseKnowledgeSource] Sync failed: {}", e.getMessage());
        }

        return documents;
    }

    @Override
    public SourceType type() {
        return SourceType.DATABASE_METADATA;
    }

    @Override
    public String sourceName(Map<String, Object> config) {
        return "Database Schema";
    }

    @Override
    public boolean isHealthy(Map<String, Object> config) {
        String jdbcUrl = str(config, "url", str(config, "jdbcUrl", ""));
        String username = str(config, "username", "");
        String password = str(config, "password", "");
        return !jdbcUrl.isBlank() && !username.isBlank() && !password.isBlank();
    }
}
