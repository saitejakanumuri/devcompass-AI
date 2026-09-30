package com.devcompass.ai.repository;

import com.devcompass.ai.model.ChunkEntity;
import com.devcompass.ai.model.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<ChunkEntity, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ChunkEntity c WHERE c.sourceType = :sourceType AND c.userId = :userId")
    void deleteBySourceTypeAndUserId(SourceType sourceType, UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChunkEntity c WHERE c.userId = :userId")
    void deleteByUserId(UUID userId);

    @Query(value = "SELECT * FROM vector_chunks ORDER BY embedding <=> cast(:embedding as vector) ASC LIMIT :topK", nativeQuery = true)
    List<ChunkEntity> findSimilarGlobal(String embedding, int topK);

    @Query(value = "SELECT * FROM vector_chunks WHERE user_id = :userId ORDER BY embedding <=> cast(:embedding as vector) ASC LIMIT :topK", nativeQuery = true)
    List<ChunkEntity> findSimilarByUserId(String embedding, UUID userId, int topK);
}
