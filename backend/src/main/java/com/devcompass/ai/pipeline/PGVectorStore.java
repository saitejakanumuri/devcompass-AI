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

            // 2. Create vector_chunks table with vector(dimension) column
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id VARCHAR(255) PRIMARY KEY,
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

    @Override
    public void saveChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;

        if (!dbInitialized || jdbcTemplate == null) {
            log.warn("[PGVectorStore] Cannot save {} chunks because PostgreSQL DB is not initialized.", chunks.size());
            return;
        }

        try {
            String insertSql = """
                INSERT INTO %s (id, document_id, document_title, source_type, content, embedding, token_count)
                VALUES (?, ?, ?, ?, ?, ?::vector, ?)
                ON CONFLICT (id) DO UPDATE SET
                    content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding,
                    token_count = EXCLUDED.token_count;
                """.formatted(tableName);

            jdbcTemplate.batchUpdate(insertSql, chunks, chunks.size(), (ps, chunk) -> {
                ps.setString(1, chunk.id());
                ps.setString(2, chunk.documentId());
                ps.setString(3, chunk.documentTitle());
                ps.setString(4, chunk.sourceType().name());
                ps.setString(5, chunk.content());
                ps.setString(6, formatVectorSql(chunk.embedding()));
                ps.setInt(7, chunk.tokenCount());
            });
            log.info("[PGVectorStore] Saved/Updated {} vector chunks in PostgreSQL table '{}'", chunks.size(), tableName);
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to save chunks to PostgreSQL table '{}'", tableName, e);
        }
    }

    @Override
    public List<Chunk> similaritySearch(String queryText, int topK, double threshold) {
        if (!dbInitialized || jdbcTemplate == null || queryText == null || queryText.isBlank()) {
            log.warn("[PGVectorStore] DB search skipped. Initialized: {}, Query: '{}'", dbInitialized, queryText);
            return List.of();
        }

        try {
            float[] queryVector = embeddingGenerator.generateEmbedding(queryText);
            String vectorStr = formatVectorSql(queryVector);

            String searchSql = """
                SELECT id, document_id, document_title, source_type, content, token_count,
                       1 - (embedding <=> ?::vector) AS score
                FROM %s
                ORDER BY embedding <=> ?::vector ASC
                LIMIT ?;
                """.formatted(tableName);

            String lowerQuery = queryText.toLowerCase();
            String[] queryWords = lowerQuery.split("[\\s\\p{Punct}]+");

            List<Chunk> rdsResults = jdbcTemplate.query(
                searchSql,
                (rs, rowNum) -> {
                    double rawVectorScore = rs.getDouble("score");
                    String content = rs.getString("content");
                    String title = rs.getString("document_title");

                    double textMatchBoost = 0.0;
                    String lowerContent = content.toLowerCase();
                    String lowerTitle = title.toLowerCase();

                    for (String word : queryWords) {
                        if (word.length() >= 2) {
                            if (lowerTitle.contains(word)) textMatchBoost += 0.35;
                            else if (lowerContent.contains(word)) textMatchBoost += 0.20;
                        }
                    }

                    double normalizedTextBoost = Math.min(1.0, textMatchBoost);
                    double adjustedVectorSim = (textMatchBoost == 0.0) ? rawVectorScore * 0.35 : rawVectorScore;
                    double finalScore = Math.min(0.99, Math.max(0.0, adjustedVectorSim * 0.4 + normalizedTextBoost * 0.6));

                    return new Chunk(
                        rs.getString("id"),
                        rs.getString("document_id"),
                        title,
                        SourceType.valueOf(rs.getString("source_type")),
                        content,
                        queryVector,
                        rs.getInt("token_count"),
                        Map.of("source", "Amazon RDS PostgreSQL pgvector"),
                        finalScore
                    );
                },
                vectorStr, vectorStr, topK * 2
            );

            List<Chunk> filtered = rdsResults.stream()
                .filter(c -> c.score() >= threshold)
                .sorted(Comparator.comparingDouble(Chunk::score).reversed())
                .limit(topK)
                .toList();

            log.info("[PGVectorStore] PostgreSQL DB semantic search returned {} chunks for query '{}'", filtered.size(), queryText);
            return filtered;
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to execute DB semantic search query on table '{}'", tableName, e);
            return List.of();
        }
    }

    @Override
    public int totalIndexedChunks() {
        if (!dbInitialized || jdbcTemplate == null) {
            return 0;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to fetch total indexed count from DB", e);
            return 0;
        }
    }

    @Override
    public void clearStore() {
        if (!dbInitialized || jdbcTemplate == null) return;
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
            log.info("[PGVectorStore] Truncated PostgreSQL table '{}'", tableName);
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to truncate table '{}'", tableName, e);
        }
    }

    @Override
    public String storeName() {
        return "Amazon RDS PostgreSQL (pgvector)";
    }

    @Override
    public boolean isInitialized() {
        return dbInitialized;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    public String getTableName() {
        return tableName();
    }

    @Override
    public void deleteChunksByDocumentId(String documentId) {
        if (!dbInitialized || jdbcTemplate == null || documentId == null || documentId.isBlank()) return;
        try {
            String deleteSql = "DELETE FROM %s WHERE document_id = ?".formatted(tableName);
            jdbcTemplate.update(deleteSql, documentId);
        } catch (Exception e) {
            log.error("[PGVectorStore] Failed to delete chunks by document_id: {}", documentId, e);
        }
    }

    private String formatVectorSql(float[] vector) {
        if (vector == null || vector.length == 0) {
            float[] empty = new float[dimension];
            return Arrays.toString(empty);
        }
        return Arrays.toString(vector);
    }
}
