package com.devcompass.ai.repository;

import com.devcompass.ai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for users. User is the top-level identity entity.
 */
@Repository
public interface AccountRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    default Optional<User> findUserByEmail(String email) {
        return findByEmail(email.toLowerCase().trim());
    }

    default Optional<User> findUserById(UUID id) {
        return findById(id);
    }

    default User createUser(String email, String passwordHash, String fullName, String role) {
        Instant now = Instant.now();
        User u = new User(UUID.randomUUID(), email, passwordHash, fullName, role != null ? role : "USER", now, now);
        return save(u);
    }
}
