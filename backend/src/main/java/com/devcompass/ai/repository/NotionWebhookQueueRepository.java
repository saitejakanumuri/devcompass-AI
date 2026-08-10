package com.devcompass.ai.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class NotionWebhookQueueRepository {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private boolean dbInitialized = false;

    public record WebhookTask(
        String id,
        String pageId,
        String eventType,
        String status,
        int retryCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
    ) {}

    @PostConstruct
    public void initSchema() {
        if (jdbcTemplate == null) return;
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS notion_webhook_queue (
                    id VARCHAR(255) PRIMARY KEY,
                    page_id VARCHAR(255) NOT NULL,
                    event_type VARCHAR(50) DEFAULT 'page_updated',
                    status VARCHAR(50) DEFAULT 'PENDING',
                    retry_count INT DEFAULT 0,
                    error_message TEXT,
                    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
                );
                """;
            jdbcTemplate.execute(createTableSql);
            dbInitialized = true;
        } catch (Exception e) {
            dbInitialized = false;
        }
    }

    public void enqueueTask(String id, String pageId, String eventType) {
        if (!dbInitialized || jdbcTemplate == null) return;

        try {
            String sql = """
                INSERT INTO notion_webhook_queue (id, page_id, event_type, status, retry_count, created_at, updated_at)
                VALUES (?, ?, ?, 'PENDING', 0, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = 'PENDING',
                    updated_at = EXCLUDED.updated_at;
                """;

            jdbcTemplate.update(
                sql,
                id,
                pageId,
                eventType != null ? eventType : "page_updated",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
            );
        } catch (Exception ignored) {}
    }

    public List<WebhookTask> fetchPendingTasks(int limit) {
        if (!dbInitialized || jdbcTemplate == null) return List.of();

        try {
            String sql = """
                SELECT id, page_id, event_type, status, retry_count, error_message, created_at, updated_at
                FROM notion_webhook_queue
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT ?;
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> new WebhookTask(
                rs.getString("id"),
                rs.getString("page_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                rs.getString("error_message"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now(),
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : Instant.now()
            ), limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    public void updateTaskStatus(String taskId, String status, String errorMessage) {
        if (!dbInitialized || jdbcTemplate == null) return;

        try {
            String sql = """
                UPDATE notion_webhook_queue
                SET status = ?,
                    error_message = ?,
                    retry_count = CASE WHEN ? = 'FAILED' THEN retry_count + 1 ELSE retry_count END,
                    updated_at = ?
                WHERE id = ?;
                """;

            jdbcTemplate.update(
                sql,
                status,
                errorMessage,
                status,
                Timestamp.from(Instant.now()),
                taskId
            );
        } catch (Exception ignored) {}
    }
}
