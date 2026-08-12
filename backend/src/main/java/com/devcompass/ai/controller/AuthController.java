package com.devcompass.ai.controller;

import com.devcompass.ai.model.AuthResponse;
import com.devcompass.ai.model.LoginRequest;
import com.devcompass.ai.model.RegisterRequest;
import com.devcompass.ai.model.User;
import com.devcompass.ai.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AccountRepository accountRepository;

    @Autowired
    public AuthController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAccount(@RequestBody RegisterRequest request) {
        if (request.email() == null || request.email().isBlank() || !request.email().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Valid email address is required."));
        }
        if (request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Password must be at least 6 characters long."));
        }

        Optional<User> existing = accountRepository.findUserByEmail(request.email());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "An account with this email address already exists."));
        }

        String passwordHash = simpleHash(request.password());
        String displayName = (request.fullName() != null && !request.fullName().isBlank())
            ? request.fullName().trim()
            : request.email().split("@")[0];

        // Create user directly — no Account entity needed
        User user = accountRepository.createUser(request.email().toLowerCase().trim(), passwordHash, displayName, "USER");
        log.info("[AuthController] Registered new user: '{}'", user.email());

        // accountId = userId for frontend backward compatibility
        String token = generateToken(user.id(), user.id());
        AuthResponse authResponse = new AuthResponse(
            token,
            user.id(),
            user.id(),          // accountId = userId (frontend compat)
            user.fullName(),    // companyName field repurposed as display name
            user.fullName(),
            user.email(),
            user.role()
        );

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Email is required."));
        }

        Optional<User> userOpt = accountRepository.findUserByEmail(request.email().trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid email address or password."));
        }

        User user = userOpt.get();
        if (!user.passwordHash().equals(simpleHash(request.password()))) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid email address or password."));
        }

        log.info("[AuthController] User logged in: '{}'", user.email());

        // accountId = userId for frontend backward compatibility
        String token = generateToken(user.id(), user.id());
        AuthResponse authResponse = new AuthResponse(
            token,
            user.id(),
            user.id(),          // accountId = userId
            user.fullName(),    // companyName = display name
            user.fullName(),
            user.email(),
            user.role()
        );

        return ResponseEntity.ok(authResponse);
    }

    private String generateToken(UUID userId, UUID accountId) {
        String payload = userId.toString() + ":" + accountId.toString() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    private String simpleHash(String raw) {
        if (raw == null) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
