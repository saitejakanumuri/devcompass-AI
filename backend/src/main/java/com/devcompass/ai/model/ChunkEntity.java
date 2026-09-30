package com.devcompass.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vector_chunks")
public class ChunkEntity {

    @Id
    private String id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "document_title", nullable = false, columnDefinition = "TEXT")
    private String documentTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "token_count")
    private int tokenCount;

    @Transient
    private double score;

    public ChunkEntity() {}

    public ChunkEntity(String id, UUID userId, String documentId, String documentTitle, SourceType sourceType, String content, float[] embedding, int tokenCount) {
        this.id = id;
        this.userId = userId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.sourceType = sourceType;
        this.content = content;
        this.embedding = embedding;
        this.tokenCount = tokenCount;
    }

    public String getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDocumentId() { return documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public SourceType getSourceType() { return sourceType; }
    public String getContent() { return content; }
    public float[] getEmbedding() { return embedding; }
    public int getTokenCount() { return tokenCount; }
    public double getScore() { return score; }

    public void setScore(double score) { this.score = score; }

    public Chunk toChunk() {
        return new Chunk(id, documentId, documentTitle, sourceType, content, embedding, tokenCount, Map.of(), score);
    }
}
