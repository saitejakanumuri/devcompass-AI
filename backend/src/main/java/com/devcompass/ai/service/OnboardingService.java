package com.devcompass.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OnboardingService {

    public List<Map<String, Object>> getOnboardingFlows() {
        return List.of(
            Map.of(
                "id", "seo-app-flow",
                "title", "SEO Application Flow & Metadata Architecture",
                "description", "Learn how tutor profiles, URL slugs, open-graph metadata, and Redis caching interact.",
                "estimatedMinutes", 10,
                "teamOwner", "Growth Engineering Team (#team-growth-eng)",
                "steps", List.of(
                    Map.of("step", 1, "name", "HTTP Request Gateway", "detail", "Client requests `/api/v1/tutors/seo/{slug}` routed through NGINX to Spring Boot."),
                    Map.of("step", 2, "name", "Tutor & SEO Lookup", "detail", "TutorSeoService queries `tutor_seo_metadata` table using canonical_slug index."),
                    Map.of("step", 3, "name", "JSON-LD & OpenGraph Compilation", "detail", "Schema.org microdata is generated dynamically."),
                    Map.of("step", 4, "name", "Redis Caching & TTL", "detail", "Rendered payload cached in Redis key `seo:tutor:{slug}` for 15m.")
                )
            ),
            Map.of(
                "id", "tutor-profile-ownership",
                "title", "Tutor Profile & Onboarding Ownership",
                "description", "Understand repository structure, service boundaries, and team assignments for Tutor Profile.",
                "estimatedMinutes", 8,
                "teamOwner", "Acquisition & Tutor Experience Team (#team-tutor-exp)",
                "steps", List.of(
                    Map.of("step", 1, "name", "Repository Overview", "detail", "Monorepo `tutor-service` located under GitHub org."),
                    Map.of("step", 2, "name", "Database Owner", "detail", "Schema `public.tutor_profiles` migrated via Flyway / Liquibase."),
                    Map.of("step", 3, "name", "CI/CD & Kubernetes", "detail", "Deployments managed via Helm chart `tutor-service` on EKS cluster.")
                )
            )
        );
    }
}
