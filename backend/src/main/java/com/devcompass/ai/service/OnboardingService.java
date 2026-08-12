package com.devcompass.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OnboardingService {

    public List<Map<String, Object>> getOnboardingFlows() {
        return List.of(
            Map.of(
                "id", "devcompass-platform-overview",
                "title", "Welcome to DevCompass AI & Platform Overview",
                "description", "Understand DevCompass AI — your intelligent developer workspace for codebase comprehension, database schema inspection, and AI RAG search.",
                "estimatedMinutes", 5,
                "teamOwner", "DevCompass Platform Team",
                "steps", List.of(
                    Map.of("step", 1, "name", "Unified Architecture Hub", "detail", "Combines Git repos, live database schemas, and Notion documentation into a single intelligent workspace."),
                    Map.of("step", 2, "name", "RAG & Vector Search", "detail", "Powered by Spring Boot AI, pgvector embeddings, and LLM orchestration for instant context-aware answers."),
                    Map.of("step", 3, "name", "Account Isolation", "detail", "Plug your own knowledge sources safely under your account context with total privacy.")
                )
            ),
            Map.of(
                "id", "advantages-of-knowledge-sources",
                "title", "Advantages of Plugging Knowledge Sources",
                "description", "Learn how connecting Git repositories, PostgreSQL/AWS RDS databases, and Notion docs unlocks deep AI insights.",
                "estimatedMinutes", 5,
                "teamOwner", "DevCompass Platform Team",
                "steps", List.of(
                    Map.of("step", 1, "name", "Git Repositories", "detail", "Index source code files to ask architectural questions, discover dependencies, and understand service boundaries."),
                    Map.of("step", 2, "name", "Target Database & AWS RDS Schemas", "detail", "Plug JDBC connection URLs to automatically explore live tables, column definitions, primary/foreign keys, and relationships without manual SQL."),
                    Map.of("step", 3, "name", "Notion & Developer Documentation", "detail", "Ingest team runbooks, design specs, and API documentation for unified knowledge search.")
                )
            ),
            Map.of(
                "id", "quickstart-guide-explore",
                "title", "How to Explore & Get Started (Quickstart)",
                "description", "Follow this 4-step quickstart guide to plug your sources and explore your application ecosystem.",
                "estimatedMinutes", 5,
                "teamOwner", "DevCompass Platform Team",
                "steps", List.of(
                    Map.of("step", 1, "name", "Step 1: Sign In or Create Account", "detail", "Sign in to access your isolated account workspace."),
                    Map.of("step", 2, "name", "Step 2: Plug Knowledge Sources", "detail", "Navigate to the Knowledge Sources tab to add Git repo URLs, PostgreSQL/AWS RDS JDBC connections, or Notion API tokens."),
                    Map.of("step", 3, "name", "Step 3: Explore Live Database Schemas", "detail", "Navigate to the Schema Explorer tab to inspect tables, column data types, and primary/foreign key constraints."),
                    Map.of("step", 4, "name", "Step 4: Ask AI Architecture Questions", "detail", "Use the AI Query tab to ask questions about your codebase, schemas, and system workflows.")
                )
            )
        );
    }
}
