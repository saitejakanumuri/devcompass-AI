package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.SourceType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("pgVectorStore")
@Primary
public class PGVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PGVectorStore.class);

    private final String tableName;
    private final int dimension;
    private final EmbeddingGenerator embeddingGenerator;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private boolean dbInitialized = false;

    public PGVectorStore(
        @Value("${devcompass.vector-store.pgvector.table:vector_chunks}") String tableName,
        @Value("${devcompass.vector-store.dimension:768}") int dimension,
        EmbeddingGenerator embeddingGenerator
    ) {
        this.tableName = tableName;
        this.dimension = dimension;
        this.embeddingGenerator = embeddingGenerator;
    }

    @PostConstruct
    public void initDatabaseSchema() {
        if (jdbcTemplate == null) {
            log.warn("[PGVectorStore] JdbcTemplate is null. PostgreSQL vector store will not initialize.");
            return;
        }

        try {
            // 1. Enable pgvector extension in Amazon RDS / PostgreSQL
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");

            // 2. Create vector_chunks table with vector(dimension) and user_id column
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(255) PRIMARY KEY,
                    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                    document_id VARCHAR(255) NOT NULL,
                    document_title TEXT NOT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(%d),
                    token_count INT DEFAULT 0,
                    created_at TIMESTAMPTZ DEFAULT NOW()
                );
                """.formatted(tableName, dimension);

            jdbcTemplate.execute(createTableSql);

            // Ensure user_id column exists and drop legacy account_id column if present
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;");
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS account_id;");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + tableName + "_user_id ON " + tableName + "(user_id);");
            } catch (Exception ignored) {}

            // 3. Create HNSW / IVFFlat vector index for fast similarity search
            String createIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_%s_embedding ON %s USING hnsw (embedding vector_cosine_ops);
                """.formatted(tableName, tableName);

            try {
                jdbcTemplate.execute(createIndexSql);
            } catch (Exception ignored) {
                // Index might already exist or require HNSW extension level
            }

            dbInitialized = true;
            log.info("[PGVectorStore] Successfully initialized PostgreSQL pgvector table: '{}' (Dimension: {})", tableName, dimension);
        } catch (Exception e) {
            dbInitialized = false;
            log.error("[PGVectorStore] Failed to initialize PostgreSQL pgvector extension or table '{}'", tableName, e);
        }
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String storeName() {
        return "Amazon RDS PostgreSQL (pgvector)";
    }

    public boolean isInitialized() {
        return dbInitialized;
    }

    @Override
    public void saveChunks(List<Chunk> chunks) {
        saveChunks(chunks, null);
    }

    public void saveChunks(List<Chunk> chunks, UUID userId) {
        if (chunks == null || chunks.isEmpty()) return;

        if (!dbInitialized || jdbcTemplate == null) {
            log.warn("[PGVectorStore] Cannot save {} chunks because PostgreSQL DB is not initialized.", chunks.size());
            return;
        }

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

    @Override
    public List<Chunk> similaritySearch(String queryText, int topK, double threshold) {
        return similaritySearch(queryText, topK, threshold, null);
    }

    public List<Chunk> similaritySearch(String queryText, int topK, double threshold, UUID userId) {
        if (!dbInitialized || jdbcTemplate == null || queryText == null || queryText.isBlank()) {
            log.warn("[PGVectorStore] DB search skipped. Initialized: {}, Query: '{}'", dbInitialized, queryText);
            return List.of();
        }

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

            log.info("[PGVectorStore] Similarity Search Query: '{}' | Candidates: {} | Returned Above Threshold (>= {}): {} (User: {})",
                queryText, retrieved.size(), threshold, filtered.size(), userId != null ? userId : "Global");

            return filtered;
        } catch (Exception e) {
            log.error("[PGVectorStore] Error during vector similarity search for query '{}'", queryText, e);
            return List.of();
        }
    }

    @Override
    public void deleteChunksByDocumentId(String documentId) {
        deleteChunksByDocumentId(documentId, null);
    }

    public void deleteChunksByDocumentId(String documentId, UUID userId) {
        if (!dbInitialized || jdbcTemplate == null || documentId == null || documentId.isBlank()) return;

        try {
            if (userId != null) {
                String sql = "DELETE FROM " + tableName + " WHERE document_id = ? AND user_id = ?;";
                int count = jdbcTemplate.update(sql, documentId, userId);
                log.info("[PGVectorStore] Deleted {} stale vector chunks for document_id '{}' (User: {})", count, documentId, userId);
            } else {
                String sql = "DELETE FROM " + tableName + " WHERE document_id = ?;";
                int count = jdbcTemplate.update(sql, documentId);
                log.info("[PGVectorStore] Deleted {} stale vector chunks for document_id '{}'", count, documentId);
            }
        } catch (Exception e) {
            log.error("[PGVectorStore] Error deleting vector chunks for document_id '{}'", documentId, e);
        }
    }

    @Override
    public void clearStore() {
        if (!dbInitialized || jdbcTemplate == null) return;
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName + ";");
            log.info("[PGVectorStore] Cleared all vector chunks from table '{}'", tableName);
        } catch (Exception e) {
            log.error("[PGVectorStore] Error clearing table '{}'", tableName, e);
        }
    }

    public void clearStoreForUser(UUID userId) {
        if (!dbInitialized || jdbcTemplate == null || userId == null) return;
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
        if (!dbInitialized || jdbcTemplate == null) return 0;
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatVectorSql(float[] vector) {
        if (vector == null) return "[]";
        return Arrays.toString(vector);
    }
}
