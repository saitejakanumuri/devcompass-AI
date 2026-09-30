package com.devcompass.ai.repository;

import com.devcompass.ai.model.AccountKnowledgeConfig;
import com.devcompass.ai.model.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Repository
public interface AccountConfigRepository extends JpaRepository<AccountKnowledgeConfig, UUID> {

    List<AccountKnowledgeConfig> findByUserId(UUID userId);

    Optional<AccountKnowledgeConfig> findByUserIdAndSourceType(UUID userId, SourceType sourceType);

    default List<AccountKnowledgeConfig> getConfigsForUser(UUID userId) {
        return findByUserId(userId);
    }

    default Optional<AccountKnowledgeConfig> getConfigForUserAndType(UUID userId, SourceType sourceType) {
        return findByUserIdAndSourceType(userId, sourceType);
    }

    @Transactional
    default AccountKnowledgeConfig saveOrUpdateConfig(UUID userId, SourceType sourceType, Map<String, Object> configJson, String status) {
        Instant now = Instant.now();

        Optional<AccountKnowledgeConfig> existing = findByUserIdAndSourceType(userId, sourceType);
        if (existing.isPresent()) {
            AccountKnowledgeConfig config = existing.get();
            config.setConfigJson(configJson);
            config.setStatus(status);
            config.setUpdatedAt(now);
            return save(config);
        }

        AccountKnowledgeConfig newConfig = new AccountKnowledgeConfig(
            UUID.randomUUID(), userId, sourceType, configJson, status, null, now, now
        );
        return save(newConfig);
    }

    @Modifying
    @Transactional
    @Query("UPDATE AccountKnowledgeConfig c SET c.lastSyncedAt = :now, c.status = :status, c.updatedAt = :now WHERE c.userId = :userId AND c.sourceType = :sourceType")
    void updateLastSyncedAtDirect(UUID userId, SourceType sourceType, String status, Instant now);

    default void updateLastSyncedAt(UUID userId, SourceType sourceType, String status) {
        updateLastSyncedAtDirect(userId, sourceType, status, Instant.now());
    }
}
