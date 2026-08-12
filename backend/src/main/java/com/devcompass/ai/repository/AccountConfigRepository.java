package com.devcompass.ai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devcompass.ai.model.AccountKnowledgeConfig;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Repository for user-scoped knowledge source configs.
 * Table: user_knowledge_configs (formerly account_knowledge_configs).
 * Isolation key: user_id only — no account_id.
 */
@Repository
public class AccountConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountConfigRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AccountConfigRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** Return all configs for this user. */
    public List<AccountKnowledgeConfig> getConfigsForUser(UUID userId) {
        String sql = """
            SELECT id, user_id, source_type, config_json, status, last_synced_at, created_at, updated_at
            FROM user_knowledge_configs
            WHERE user_id = ?;
            """;
        try {
            return jdbcTemplate.query(sql, this::mapConfig, userId);
        } catch (Exception e) {
            log.error("[AccountConfigRepository] Error fetching configs for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Kept for backward compat with AccountConfigController that still accepts accountId/userId pair.
     * Since accountId == userId in the flattened schema, we just use userId.
     */
    public List<AccountKnowledgeConfig> getEffectiveConfigsForAccountAndUser(UUID accountId, UUID userId) {
        UUID effectiveId = userId != null ? userId : accountId;
        return getConfigsForUser(effectiveId);
    }

    public Optional<AccountKnowledgeConfig> getConfigForAccountAndType(UUID accountId, SourceType sourceType) {
        return getConfigForUserAndType(accountId, sourceType);
    }

    public Optional<AccountKnowledgeConfig> getConfigForUserAndType(UUID userId, SourceType sourceType) {
        return getConfigsForUser(userId).stream()
            .filter(c -> c.sourceType() == sourceType)
            .findFirst();
    }

    /** getConfigForAccountUserAndType — backward compat, uses userId or accountId as isolation key */
    public Optional<AccountKnowledgeConfig> getConfigForAccountUserAndType(UUID accountId, UUID userId, SourceType sourceType) {
        UUID effectiveId = userId != null ? userId : accountId;
        return getConfigForUserAndType(effectiveId, sourceType);
    }

    public AccountKnowledgeConfig saveOrUpdateConfig(UUID userId, SourceType sourceType, Map<String, Object> configJson, String status) {
        return saveOrUpdateUserConfig(userId, null, sourceType, configJson, status);
    }

    /**
     * Save or update a config for this user.
     * The second userId param is ignored (legacy compat shim — was user override vs account-level).
     * In the flat schema there is only one userId.
     */
    public AccountKnowledgeConfig saveOrUpdateUserConfig(UUID accountId, UUID userIdOverride, SourceType sourceType, Map<String, Object> configJson, String status) {
        UUID userId = userIdOverride != null ? userIdOverride : accountId;
        Instant now = Instant.now();
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(configJson);
        } catch (JsonProcessingException e) {
            jsonString = "{}";
        }

        String checkSql = "SELECT id FROM user_knowledge_configs WHERE user_id = ? AND source_type = ?";
        List<UUID> existingIds = jdbcTemplate.query(checkSql,
            (rs, r) -> UUID.fromString(rs.getString("id")), userId, sourceType.name());

        if (!existingIds.isEmpty()) {
            UUID configId = existingIds.get(0);
            String updateSql = """
                UPDATE user_knowledge_configs
                SET config_json = ?::jsonb, status = ?, updated_at = ?
                WHERE id = ?
                RETURNING id, user_id, source_type, config_json, status, last_synced_at, created_at, updated_at;
                """;
            try {
                return jdbcTemplate.queryForObject(updateSql, this::mapConfig, jsonString, status, Timestamp.from(now), configId);
            } catch (Exception e) {
                log.warn("[AccountConfigRepository] Fallback after update: {}", e.getMessage());
            }
        }

        UUID configId = UUID.randomUUID();
        String insertSql = """
            INSERT INTO user_knowledge_configs (id, user_id, source_type, config_json, status, created_at, updated_at)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
            RETURNING id, user_id, source_type, config_json, status, last_synced_at, created_at, updated_at;
            """;
        try {
            return jdbcTemplate.queryForObject(insertSql, this::mapConfig,
                configId, userId, sourceType.name(), jsonString, status,
                Timestamp.from(now), Timestamp.from(now));
        } catch (Exception e) {
            log.error("[AccountConfigRepository] Error inserting config for user {} source {}: {}", userId, sourceType, e.getMessage());
            return new AccountKnowledgeConfig(configId, userId, sourceType, configJson, status, null, now, now);
        }
    }

    public void updateLastSyncedAt(UUID userId, SourceType sourceType, String status) {
        // compat: accountId may be passed as first arg — treat as userId
        String sql = """
            UPDATE user_knowledge_configs
            SET last_synced_at = ?, status = ?, updated_at = ?
            WHERE user_id = ? AND source_type = ?;
            """;
        Instant now = Instant.now();
        jdbcTemplate.update(sql, Timestamp.from(now), status, Timestamp.from(now), userId, sourceType.name());
    }

    private AccountKnowledgeConfig mapConfig(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> configMap = new HashMap<>();
        String jsonStr = rs.getString("config_json");
        if (jsonStr != null && !jsonStr.isBlank()) {
            try {
                configMap = objectMapper.readValue(jsonStr, Map.class);
            } catch (Exception ignored) {}
        }

        Timestamp lastSyncedTs = rs.getTimestamp("last_synced_at");

        return new AccountKnowledgeConfig(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("user_id")),
            SourceType.valueOf(rs.getString("source_type")),
            configMap,
            rs.getString("status"),
            lastSyncedTs != null ? lastSyncedTs.toInstant() : null,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
