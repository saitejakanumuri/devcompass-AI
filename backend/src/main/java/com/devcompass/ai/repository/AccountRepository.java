package com.devcompass.ai.repository;

import com.devcompass.ai.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Repository for users. User is the top-level identity entity.
 */
@Repository
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        if (jdbcTemplate == null) return;
        try {
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

            log.info("[UserRepo] Schema initialized.");
        } catch (Exception e) {
            log.warn("[UserRepo] Schema init skipped (may already exist): {}", e.getMessage());
        }
    }

    public User createUser(String email, String passwordHash, String fullName, String role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update(
            "INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, email, passwordHash, fullName, role != null ? role : "USER", Timestamp.from(now), Timestamp.from(now)
        );
        log.info("[UserRepo] Created user: '{}'", email);
        return new User(id, email, passwordHash, fullName, role != null ? role : "USER", now, now);
    }

    public Optional<User> findUserByEmail(String email) {
        try {
            User user = jdbcTemplate.queryForObject(
                "SELECT id, email, password_hash, full_name, role, created_at, updated_at FROM users WHERE email = ?",
                this::mapUser, email.toLowerCase().trim()
            );
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findUserById(UUID id) {
        try {
            User user = jdbcTemplate.queryForObject(
                "SELECT id, email, password_hash, full_name, role, created_at, updated_at FROM users WHERE id = ?",
                this::mapUser, id
            );
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
