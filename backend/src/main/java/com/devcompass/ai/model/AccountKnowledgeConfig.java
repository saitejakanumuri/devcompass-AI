package com.devcompass.ai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a user's per-source knowledge configuration (formerly AccountKnowledgeConfig).
 * Keyed by user_id — each user manages their own source connections.
 */
@Entity
@Table(name = "user_knowledge_configs")
public class AccountKnowledgeConfig {
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configJson;
    
    private String status;
    
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;

    public AccountKnowledgeConfig() {}

    public AccountKnowledgeConfig(UUID id, UUID userId, SourceType sourceType, Map<String, Object> configJson, String status, Instant lastSyncedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.sourceType = sourceType;
        this.configJson = configJson;
        this.status = status;
        this.lastSyncedAt = lastSyncedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public SourceType sourceType() { return sourceType; }
    public Map<String, Object> configJson() { return configJson; }
    public String status() { return status; }
    public Instant lastSyncedAt() { return lastSyncedAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public Map<String, Object> getConfigJson() { return configJson; }
    public void setConfigJson(Map<String, Object> configJson) { this.configJson = configJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
