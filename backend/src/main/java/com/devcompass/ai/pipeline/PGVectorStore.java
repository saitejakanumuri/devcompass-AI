package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Chunk;
import com.devcompass.ai.model.SourceType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component("pgVectorStore")
@Primary
public class PGVectorStore implements VectorStore {

    private final String tableName;
    private final int dimension;
    private final InMemoryVectorStore delegateStore;
    private final EmbeddingGenerator embeddingGenerator;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private boolean dbInitialized = false;

    public PGVectorStore(
        @Value("${devcompass.vector-store.pgvector.table:vector_chunks}") String tableName,
        @Value("${devcompass.vector-store.dimension:768}") int dimension,
        InMemoryVectorStore delegateStore,
        EmbeddingGenerator embeddingGenerator
    ) {
        this.tableName = tableName;
        this.dimension = dimension;
        this.delegateStore = delegateStore;
        this.embeddingGenerator = embeddingGenerator;
    }

    @PostConstruct
    public void initDatabaseSchema() {
        if (jdbcTemplate == null) {
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
        } catch (Exception e) {
            // Database offline or test profile - will use in-memory fallback
            dbInitialized = false;
        }
    }

    @Override
    public void saveChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;

        // Always update delegate in-memory store for fallback
        delegateStore.saveChunks(chunks);

        if (!dbInitialized || jdbcTemplate == null) {
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
        } catch (Exception e) {
            // Log fallback when RDS PostgreSQL is unreachable
        }
    }

    @Override
    public List<Chunk> similaritySearch(String queryText, int topK, double threshold) {
        if (dbInitialized && jdbcTemplate != null) {
            try {
                float[] queryVector = embeddingGenerator.generateEmbedding(queryText);
                String vectorStr = formatVectorSql(queryVector);

                String searchSql = """
                    SELECT id, document_id, document_title, source_type, content, token_count,
                           1 - (embedding <=> ?::vector) AS score
                    FROM %s
                    WHERE 1 - (embedding <=> ?::vector) >= ?
                    ORDER BY embedding <=> ?::vector ASC
                    LIMIT ?;
                    """.formatted(tableName);

                List<Chunk> rdsResults = jdbcTemplate.query(
                    searchSql,
                    (rs, rowNum) -> new Chunk(
                        rs.getString("id"),
                        rs.getString("document_id"),
                        rs.getString("document_title"),
                        SourceType.valueOf(rs.getString("source_type")),
                        rs.getString("content"),
                        queryVector,
                        rs.getInt("token_count"),
                        Map.of("source", "Amazon RDS PostgreSQL pgvector"),
                        rs.getDouble("score")
                    ),
                    vectorStr, vectorStr, threshold, topK
                );

                if (!rdsResults.isEmpty()) {
                    return rdsResults;
                }
            } catch (Exception e) {
                // Fallback to delegate store if RDS query encounters connection/syntax issues
            }
        }

        return delegateStore.similaritySearch(queryText, topK, threshold);
    }

    @Override
    public int totalIndexedChunks() {
        if (dbInitialized && jdbcTemplate != null) {
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
                if (count != null && count > 0) return count;
            } catch (Exception ignored) {}
        }
        return delegateStore.totalIndexedChunks();
    }

    @Override
    public void clearStore() {
        delegateStore.clearStore();
        if (dbInitialized && jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
            } catch (Exception ignored) {}
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
        if (documentId == null || documentId.isBlank()) return;

        delegateStore.deleteChunksByDocumentId(documentId);

        if (!dbInitialized || jdbcTemplate == null) return;

        try {
            String deleteSql = "DELETE FROM %s WHERE document_id = ?".formatted(tableName);
            jdbcTemplate.update(deleteSql, documentId);
        } catch (Exception ignored) {}
    }

    private String formatVectorSql(float[] vector) {
        if (vector == null || vector.length == 0) {
            float[] empty = new float[dimension];
            return Arrays.toString(empty);
        }
        return Arrays.toString(vector);
    }
}
