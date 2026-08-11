package com.devcompass.ai.repository;

import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.model.SyncTrackerRecord;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class SyncTrackerRepository {

    private static final Logger log = LoggerFactory.getLogger(SyncTrackerRepository.class);

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private boolean dbInitialized = false;

    @PostConstruct
    public void initSchema() {
        if (jdbcTemplate == null) return;
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS knowledge_sync_tracker (
                    doc_id VARCHAR(255) NOT NULL,
                    doc_title TEXT NOT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    external_source_id VARCHAR(255) PRIMARY KEY,
                    last_edited_time TIMESTAMPTZ NOT NULL,
                    last_embedded_date TIMESTAMPTZ NOT NULL,
                    chunk_count INT DEFAULT 0,
                    status VARCHAR(50) DEFAULT 'SYNCED'
                );
                """;
            jdbcTemplate.execute(createTableSql);

            // Deduplicate legacy entries keeping only the latest last_embedded_date entry per external_source_id
            try {
                String dedupeSql = """
                    DELETE FROM knowledge_sync_tracker a
                    WHERE a.ctid IN (
                        SELECT ctid FROM (
                            SELECT ctid, ROW_NUMBER() OVER (
                                PARTITION BY external_source_id 
                                ORDER BY last_embedded_date DESC, last_edited_time DESC
                            ) as rn
                            FROM knowledge_sync_tracker
                        ) t
                        WHERE t.rn > 1
                    );
                    """;
                jdbcTemplate.execute(dedupeSql);

                String createIndexSql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_tracker_ext_id ON knowledge_sync_tracker (external_source_id);";
                jdbcTemplate.execute(createIndexSql);
            } catch (Exception e) {
                log.warn("[SyncTrackerRepository] Unique index creation or deduplication skipped: {}", e.getMessage());
            }

            dbInitialized = true;
            log.info("[SyncTrackerRepository] Successfully initialized knowledge_sync_tracker table schema.");
        } catch (Exception e) {
            dbInitialized = false;
            log.error("[SyncTrackerRepository] Failed to initialize knowledge_sync_tracker schema: {}", e.getMessage());
        }
    }

    public boolean shouldReembed(String externalSourceId, Instant lastEditedTime) {
        if (!dbInitialized || jdbcTemplate == null || lastEditedTime == null) {
            return true; // Always embed if tracker database is offline
        }

        try {
            String sql = "SELECT last_embedded_date FROM knowledge_sync_tracker WHERE external_source_id = ? ORDER BY last_embedded_date DESC LIMIT 1";
            List<Timestamp> timestamps = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getTimestamp("last_embedded_date"), externalSourceId);

            if (timestamps.isEmpty()) {
                return true; // New document - must embed!
            }

            Instant lastEmbeddedDate = timestamps.get(0).toInstant();
            // Re-embed ONLY if modified time > last embedded date
            return lastEditedTime.isAfter(lastEmbeddedDate);
        } catch (Exception e) {
            log.warn("[SyncTrackerRepository] shouldReembed query failed for {}: {}", externalSourceId, e.getMessage());
            return true;
        }
    }

    public void upsertSyncRecord(SyncTrackerRecord record) {
        if (!dbInitialized || jdbcTemplate == null) return;

        try {
            String sql = """
                INSERT INTO knowledge_sync_tracker 
                (doc_id, doc_title, source_type, external_source_id, last_edited_time, last_embedded_date, chunk_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (external_source_id) DO UPDATE SET
                    doc_id = EXCLUDED.doc_id,
                    doc_title = EXCLUDED.doc_title,
                    source_type = EXCLUDED.source_type,
                    last_edited_time = EXCLUDED.last_edited_time,
                    last_embedded_date = EXCLUDED.last_embedded_date,
                    chunk_count = EXCLUDED.chunk_count,
                    status = EXCLUDED.status;
                """;

            jdbcTemplate.update(
                sql,
                record.docId(),
                record.docTitle(),
                record.sourceType().name(),
                record.externalSourceId(),
                Timestamp.from(record.lastEditedTime() != null ? record.lastEditedTime() : Instant.now()),
                Timestamp.from(record.lastEmbeddedDate() != null ? record.lastEmbeddedDate() : Instant.now()),
                record.chunkCount(),
                record.status()
            );
            log.info("[SyncTrackerRepository] Successfully upserted tracking record for external_source_id '{}' (status: {})", 
                record.externalSourceId(), record.status());
        } catch (Exception e) {
            log.error("[SyncTrackerRepository] Failed to upsert tracking record for external_source_id '{}': {}", 
                record.externalSourceId(), e.getMessage(), e);
        }
    }

    public List<SyncTrackerRecord> listAllTrackedDocuments() {
        if (!dbInitialized || jdbcTemplate == null) return List.of();

        try {
            String sql = "SELECT doc_id, doc_title, source_type, external_source_id, last_edited_time, last_embedded_date, chunk_count, status FROM knowledge_sync_tracker ORDER BY last_embedded_date DESC";
            return jdbcTemplate.query(sql, (rs, rowNum) -> new SyncTrackerRecord(
                rs.getString("doc_id"),
                rs.getString("doc_title"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("external_source_id"),
                rs.getTimestamp("last_edited_time").toInstant(),
                rs.getTimestamp("last_embedded_date").toInstant(),
                rs.getInt("chunk_count"),
                rs.getString("status")
            ));
        } catch (Exception e) {
            log.error("[SyncTrackerRepository] Failed to list tracked documents: {}", e.getMessage());
            return List.of();
        }
    }
}
