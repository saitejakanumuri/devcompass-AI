package com.devcompass.ai.repository;

import com.devcompass.ai.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for users.
 * Users are the top-level identity — no parent Account entity exists anymore.
 */
@Repository
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        if (jdbcTemplate == null) return;
        try {
            // Users table — no account_id FK (flattened schema)
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(150) NOT NULL,
                    role VARCHAR(50) DEFAULT 'USER',
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                """);

            // Per-user knowledge source configs
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_knowledge_configs (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    source_type VARCHAR(50) NOT NULL,
                    config_json JSONB NOT NULL,
                    status VARCHAR(50) DEFAULT 'UNCONFIGURED',
                    last_synced_at TIMESTAMP WITH TIME ZONE,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                """);

            // Vector chunks store with user-level isolation
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vector_chunks (
                    id VARCHAR(255) PRIMARY KEY,
                    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                    document_id VARCHAR(255) NOT NULL,
                    document_title TEXT NOT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(768),
                    token_count INT,
                    metadata JSONB,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                """);

            log.info("[AccountRepository] User schema initialized successfully.");
        } catch (Exception e) {
            log.warn("[AccountRepository] Could not initialize user schema (may already exist): {}", e.getMessage());
        }
    }

    /**
     * Create a new user. No Account is created — user is the top-level identity.
     * Returns accountId = userId for frontend compatibility.
     */
    public User createUser(String email, String passwordHash, String fullName, String role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        String sql = """
            INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql, id, email, passwordHash, fullName,
            role != null ? role : "USER", Timestamp.from(now), Timestamp.from(now));
        log.info("[AccountRepository] Created user: '{}'", email);
        return new User(id, email, passwordHash, fullName, role != null ? role : "USER", now, now);
    }

    public Optional<User> findUserByEmail(String email) {
        String sql = "SELECT id, email, password_hash, full_name, role, created_at, updated_at FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapUser, email.toLowerCase().trim());
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findUserById(UUID id) {
        String sql = "SELECT id, email, password_hash, full_name, role, created_at, updated_at FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapUser, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private User mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new User(
            UUID.fromString(rs.getString("id")),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            rs.getString("role"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
