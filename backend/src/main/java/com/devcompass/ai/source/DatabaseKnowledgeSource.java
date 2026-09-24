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

    private String jdbcUrl = "";
    private String username = "";
    private String password = "";

    public void updateConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public List<Document> sync() {
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
    public String sourceName() {
        return "Database Schema";
    }

    @Override
    public boolean isHealthy() {
        return !jdbcUrl.isBlank() && !username.isBlank() && !password.isBlank();
    }
}
