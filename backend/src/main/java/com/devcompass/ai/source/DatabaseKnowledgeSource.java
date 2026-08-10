package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DatabaseKnowledgeSource implements KnowledgeSource {

    @Override
    public List<Document> sync() {
        return List.of(
            new Document(
                UUID.randomUUID().toString(),
                "INFORMATION_SCHEMA - tutor_seo_metadata Table Definition",
                "db-schema-tutor_seo_metadata",
                SourceType.DATABASE_METADATA,
                """
                TABLE: public.tutor_seo_metadata
                COLUMNS:
                - id UUID PRIMARY KEY DEFAULT gen_random_uuid()
                - tutor_id UUID NOT NULL REFERENCES public.tutor_profiles(id)
                - canonical_slug VARCHAR(255) UNIQUE NOT NULL
                - meta_title VARCHAR(150) NOT NULL
                - meta_description TEXT
                - schema_jsonld JSONB NOT NULL
                - created_at TIMESTAMPTZ DEFAULT NOW()
                - updated_at TIMESTAMPTZ DEFAULT NOW()
                
                INDEXES:
                - idx_seo_slug ON public.tutor_seo_metadata (canonical_slug)
                - idx_seo_tutor ON public.tutor_seo_metadata (tutor_id)
                """,
                Map.of("database", "production_tutor_db", "schema", "public", "engine", "PostgreSQL 16")
            ),
            new Document(
                UUID.randomUUID().toString(),
                "INFORMATION_SCHEMA - tutor_profiles Table Definition",
                "db-schema-tutor_profiles",
                SourceType.DATABASE_METADATA,
                """
                TABLE: public.tutor_profiles
                COLUMNS:
                - id UUID PRIMARY KEY DEFAULT gen_random_uuid()
                - full_name VARCHAR(100) NOT NULL
                - bio TEXT
                - hourly_rate NUMERIC(10,2) NOT NULL
                - rating NUMERIC(3,2) DEFAULT 5.00
                - active_status VARCHAR(20) DEFAULT 'ACTIVE'
                """,
                Map.of("database", "production_tutor_db", "schema", "public", "engine", "PostgreSQL 16")
            )
        );
    }

    @Override
    public SourceType type() {
        return SourceType.DATABASE_METADATA;
    }

    @Override
    public String sourceName() {
        return "PostgreSQL Database INFORMATION_SCHEMA";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
