package com.devcompass.ai.controller;

import com.devcompass.ai.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/flows")
    public ResponseEntity<List<Map<String, Object>>> getOnboardingFlows() {
        return ResponseEntity.ok(onboardingService.getOnboardingFlows());
    }
}
