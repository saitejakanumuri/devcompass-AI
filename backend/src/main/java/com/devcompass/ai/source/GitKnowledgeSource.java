package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GitKnowledgeSource implements KnowledgeSource {

    @Override
    public List<Document> sync() {
        return List.of(
            new Document(
                UUID.randomUUID().toString(),
                "TutorSeoController.java - Spring Boot REST Endpoints",
                "git-repo-tutor-service/TutorSeoController.java",
                SourceType.GIT_REPOSITORY,
                """
                package com.company.tutor.controller;

                import com.company.tutor.service.TutorSeoService;
                import org.springframework.web.bind.annotation.*;

                @RestController
                @RequestMapping("/api/v1/tutors/seo")
                public class TutorSeoController {

                    private final TutorSeoService seoService;

                    public TutorSeoController(TutorSeoService seoService) {
                        this.seoService = seoService;
                    }

                    @GetMapping("/{slug}")
                    public TutorSeoMetaResponse getSeoMetadata(@PathVariable String slug) {
                        return seoService.resolveSeoForSlug(slug);
                    }
                }
                """,
                Map.of("repo", "tutor-service", "branch", "main", "commit", "a1b2c3d4")
            ),
            new Document(
                UUID.randomUUID().toString(),
                "deploy.yml - Kubernetes & Helm Deployment Workflow",
                "git-repo-infra/deploy.yml",
                SourceType.GIT_REPOSITORY,
                """
                name: CI/CD Deployment Pipeline

                on:
                  push:
                    branches: [ main ]

                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    steps:
                    - uses: actions/checkout@v4
                    - name: Build Docker Container
                      run: docker build -t tutor-seo-service:${{ github.sha }} .
                    - name: Deploy to Kubernetes via Helm
                      run: helm upgrade --install tutor-seo ./helm -f values-prod.yaml
                """,
                Map.of("repo", "infra-deployments", "branch", "main", "commit", "f9e8d7c6")
            )
        );
    }

    @Override
    public SourceType type() {
        return SourceType.GIT_REPOSITORY;
    }

    @Override
    public String sourceName() {
        return "Git Repositories (tutor-service & infra-deployments)";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
