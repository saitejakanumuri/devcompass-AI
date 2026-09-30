package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("pgVectorStore")
@Primary
public class PGVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PGVectorStore.class);

    private final EmbeddingGenerator embeddingGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public PGVectorStore(EmbeddingGenerator embeddingGenerator, JdbcTemplate jdbcTemplate, @Value("${devcompass.vector-store.pgvector.table:vector_chunks}") String tableName) {
        this.embeddingGenerator = embeddingGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public String storeName() {
        return "Amazon RDS PostgreSQL (pgvector via JdbcTemplate)";
    }

    @Override
    public boolean isInitialized() {
        return true; // Table schema is managed by Hibernate via ChunkEntity
    }

    @Override
    public void saveChunks(List<Chunk> chunks) {
        saveChunks(chunks, null);
    }

    public void saveChunks(List<Chunk> chunks, UUID userId) {
        if (chunks == null || chunks.isEmpty()) return;
        if (jdbcTemplate == null) return;
        try {
            String insertSql = """
                INSERT INTO %s (id, user_id, document_id, document_title, source_type, content, embedding, token_count)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?)
                ON CONFLICT (id) DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding,
                    token_count = EXCLUDED.token_count;
                """.formatted(tableName);

            jdbcTemplate.batchUpdate(insertSql, chunks, chunks.size(), (ps, chunk) -> {
                ps.setString(1, chunk.id());
                if (userId != null) {
                    ps.setObject(2, userId);
                } else {
                    ps.setObject(2, null);
                }
                ps.setString(3, chunk.documentId());
                ps.setString(4, chunk.documentTitle());
                ps.setString(5, chunk.sourceType().name());
                ps.setString(6, chunk.content());
                ps.setString(7, formatVectorSql(chunk.embedding()));
                ps.setInt(8, chunk.tokenCount());
            });
            log.info("[PGVectorStore] Saved/Updated {} vector chunks in PostgreSQL table '{}' (User: {})", chunks.size(), tableName, userId != null ? userId : "Global");
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to save chunks to PostgreSQL table '{}'", tableName, e);
        }
    }

    private String formatVectorSql(float[] vector) {
        if (vector == null) return "[]";
        return Arrays.toString(vector);
    }

    @Override
    public List<Chunk> similaritySearch(String queryText, int topK, double threshold) {
        return similaritySearch(queryText, topK, threshold, null);
    }

    public List<Chunk> similaritySearch(String queryText, int topK, double threshold, UUID userId) {
        if (jdbcTemplate == null || queryText == null || queryText.isBlank()) {
            return List.of();
        }
        log.info("userID:: "+userId);

        try {
            float[] queryVector = embeddingGenerator.generateEmbedding(queryText);
            String vectorStr = formatVectorSql(queryVector);

            String searchSql;
            Object[] args;

            if (userId != null) {
                searchSql = """
                    SELECT id, document_id, document_title, source_type, content, token_count,
                           1 - (embedding <=> ?::vector) AS score
                    FROM %s
                    WHERE user_id = ?
                    ORDER BY embedding <=> ?::vector ASC
                    LIMIT ?;
                    """.formatted(tableName);
                args = new Object[]{vectorStr, userId, vectorStr, topK};
            } else {
                searchSql = """
                    SELECT id, document_id, document_title, source_type, content, token_count,
                           1 - (embedding <=> ?::vector) AS score
                    FROM %s
                    ORDER BY embedding <=> ?::vector ASC
                    LIMIT ?;
                    """.formatted(tableName);
                args = new Object[]{vectorStr, vectorStr, topK};
            }

            List<Chunk> retrieved = jdbcTemplate.query(searchSql, (rs, rowNum) -> {
                double score = rs.getDouble("score");
                return new Chunk(
                    rs.getString("id"),
                    rs.getString("document_id"),
                    rs.getString("document_title"),
                    SourceType.valueOf(rs.getString("source_type")),
                    rs.getString("content"),
                    new float[0],
                    rs.getInt("token_count"),
                    Map.of(),
                    score
                );
            }, args);

            List<Chunk> filtered = retrieved.stream()
                .filter(c -> c.score() >= threshold)
                .sorted(Comparator.comparingDouble(Chunk::score).reversed())
                .toList();

            log.info("[PGVectorStore] Search '{}' | Threshold >= {} | Returned: {} (User: {})", queryText, threshold, filtered.size(), userId);
            return filtered;
        } catch (Exception e) {
            log.error("[PGVectorStore] Error during vector similarity search for query '{}'", queryText, e);
            return List.of();
        }
    }

    @Override
    public void deleteChunksBySourceType(SourceType sourceType, UUID userId) {
        if (jdbcTemplate == null || sourceType == null || userId == null) return;
        try {
            String sql = "DELETE FROM " + tableName + " WHERE source_type = ? AND user_id = ?;";
            int count = jdbcTemplate.update(sql, sourceType.name(), userId);
            log.info("[PGVectorStore] Deleted {} stale vector chunks for source_type '{}' (User: {})", count, sourceType, userId);
        } catch (Exception e) {
            log.error("[PGVectorStore] Error deleting vector chunks for source_type '{}'", sourceType, e);
        }
    }

    @Override
    public void clearStore() {
        if (jdbcTemplate == null) return;
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName + ";");
            log.info("[PGVectorStore] Cleared all vector chunks");
        } catch (Exception e) {
            log.error("[PGVectorStore] Error clearing table", e);
        }
    }

    public void clearStoreForUser(UUID userId) {
        if (jdbcTemplate == null || userId == null) return;
        try {
            String sql = "DELETE FROM " + tableName + " WHERE user_id = ?;";
            int count = jdbcTemplate.update(sql, userId);
            log.info("[PGVectorStore] Cleared {} vector chunks for user ID: {}", count, userId);
        } catch (Exception e) {
            log.error("[PGVectorStore] Error clearing table for user ID: {}", userId, e);
        }
    }

    @Override
    public int totalIndexedChunks() {
        if (jdbcTemplate == null) return 0;
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
