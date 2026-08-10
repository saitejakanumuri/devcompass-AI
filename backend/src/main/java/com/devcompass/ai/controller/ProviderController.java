package com.devcompass.ai.controller;

import com.devcompass.ai.model.ProviderConfig;
import com.devcompass.ai.provider.AIProviderFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final AIProviderFactory providerFactory;

    public ProviderController(AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @GetMapping
    public ResponseEntity<List<ProviderConfig>> listProviders() {
        return ResponseEntity.ok(providerFactory.listProviders());
    }

    @PostMapping("/active")
    public ResponseEntity<Map<String, String>> setActiveProvider(@RequestBody Map<String, String> body) {
        String providerName = body.get("provider");
        if (providerName == null || providerName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            providerFactory.setActiveProvider(providerName);
            return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "activeProvider", providerFactory.getActiveProvider().providerName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
