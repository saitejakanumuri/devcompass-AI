package com.devcompass.ai.controller;

import com.devcompass.ai.model.AuthResponse;
import com.devcompass.ai.model.LoginRequest;
import com.devcompass.ai.model.RegisterRequest;
import com.devcompass.ai.model.User;
import com.devcompass.ai.repository.AccountRepository;
import com.devcompass.ai.security.AuthenticatedUser;
import com.devcompass.ai.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AccountRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AccountRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.email() == null || request.email().isBlank() || !request.email().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Valid email is required."));
        }
        if (request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Password must be at least 6 characters."));
        }

        Optional<User> existing = userRepository.findUserByEmail(request.email());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "An account with this email already exists."));
        }

        String displayName = (request.fullName() != null && !request.fullName().isBlank())
            ? request.fullName().trim()
            : request.email().split("@")[0];

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.createUser(request.email().toLowerCase().trim(), hashedPassword, displayName, "USER");

        log.info("[Auth] Registered user: '{}'", user.email());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.id(), user.fullName(), user.email(), user.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Email is required."));
        }

        Optional<User> userOpt = userRepository.findUserByEmail(request.email().trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid email or password."));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid email or password."));
        }

        log.info("[Auth] User logged in: '{}'", user.email());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.id(), user.fullName(), user.email(), user.role()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userRepository.findUserById(principal.userId())
            .map(user -> ResponseEntity.ok(new AuthResponse(null, user.id(), user.fullName(), user.email(), user.role())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userRepository.findUserById(principal.userId())
            .map(user -> {
                String newToken = jwtService.generateToken(user);
                return ResponseEntity.ok(new AuthResponse(newToken, user.id(), user.fullName(), user.email(), user.role()));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
