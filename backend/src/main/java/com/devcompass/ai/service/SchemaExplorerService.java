package com.devcompass.ai.service;

import com.devcompass.ai.model.SchemaMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SchemaExplorerService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private final List<SchemaMetadata> fallbackSchemas = List.of(
        new SchemaMetadata(
            "public",
            "tutor_seo_metadata",
            "Stores canonical URLs, meta tags, and structured Schema.org JSON-LD microdata for tutor profile pages.",
            List.of(
                new SchemaMetadata.ColumnMetadata("id", "UUID", false, "Primary key generated via gen_random_uuid()"),
                new SchemaMetadata.ColumnMetadata("tutor_id", "UUID", false, "Foreign key reference to public.tutor_profiles(id)"),
                new SchemaMetadata.ColumnMetadata("canonical_slug", "VARCHAR(255)", false, "Unique SEO-optimized URL path slug"),
                new SchemaMetadata.ColumnMetadata("meta_title", "VARCHAR(150)", false, "HTML head title tag value"),
                new SchemaMetadata.ColumnMetadata("meta_description", "TEXT", true, "HTML meta description tag value"),
                new SchemaMetadata.ColumnMetadata("schema_jsonld", "JSONB", false, "Structured JSON-LD schema snippet for Google Search"),
                new SchemaMetadata.ColumnMetadata("created_at", "TIMESTAMPTZ", false, "Creation timestamp"),
                new SchemaMetadata.ColumnMetadata("updated_at", "TIMESTAMPTZ", false, "Last update timestamp")
            ),
            List.of("id"),
            List.of(new SchemaMetadata.ForeignKeyMetadata("tutor_id", "tutor_profiles", "id")),
            true
        ),
        new SchemaMetadata(
            "public",
            "tutor_profiles",
            "Main profile entity table containing tutor personal details, hourly rate, rating, and status.",
            List.of(
                new SchemaMetadata.ColumnMetadata("id", "UUID", false, "Primary key identifier"),
                new SchemaMetadata.ColumnMetadata("full_name", "VARCHAR(100)", false, "Full displayed name"),
                new SchemaMetadata.ColumnMetadata("bio", "TEXT", true, "Tutor introduction and background"),
                new SchemaMetadata.ColumnMetadata("hourly_rate", "NUMERIC(10,2)", false, "Hourly billing rate in USD"),
                new SchemaMetadata.ColumnMetadata("rating", "NUMERIC(3,2)", false, "Average student review rating (0.00 to 5.00)"),
                new SchemaMetadata.ColumnMetadata("active_status", "VARCHAR(20)", false, "Status: ACTIVE, INACTIVE, PENDING_VERIFICATION")
            ),
            List.of("id"),
            List.of(),
            true
        ),
        new SchemaMetadata(
            "public",
            "seo_slug_mappings",
            "Maps dynamic incoming request slugs to backend tutor IDs and locale settings.",
            List.of(
                new SchemaMetadata.ColumnMetadata("slug_id", "UUID", false, "Primary key"),
                new SchemaMetadata.ColumnMetadata("source_path", "VARCHAR(500)", false, "Incoming relative HTTP request path"),
                new SchemaMetadata.ColumnMetadata("target_tutor_id", "UUID", false, "Target tutor ID"),
                new SchemaMetadata.ColumnMetadata("redirect_code", "INT", false, "HTTP status code (200, 301, 302)")
            ),
            List.of("slug_id"),
            List.of(new SchemaMetadata.ForeignKeyMetadata("target_tutor_id", "tutor_profiles", "id")),
            true
        )
    );

    public List<SchemaMetadata> getAllSchemas() {
        if (jdbcTemplate != null) {
            try {
                List<SchemaMetadata> liveSchemas = fetchLiveSchemasFromRds();
                if (liveSchemas != null && !liveSchemas.isEmpty()) {
                    return liveSchemas;
                }
            } catch (Exception e) {
                // Fallback gracefully if RDS is unreachable or during offline unit testing
            }
        }

        return fallbackSchemas;
    }

    public Optional<SchemaMetadata> getSchemaForTable(String tableName) {
        return getAllSchemas().stream()
            .filter(s -> s.tableName().equalsIgnoreCase(tableName))
            .findFirst();
    }

    private List<SchemaMetadata> fetchLiveSchemasFromRds() {
        String tablesSql = """
            SELECT table_schema, table_name
            FROM information_schema.tables
            WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
              AND table_type = 'BASE TABLE'
            ORDER BY table_name;
            """;

        List<SchemaMetadata> result = new ArrayList<>();

        List<TableRef> tableRefs = jdbcTemplate.query(
            tablesSql,
            (rs, rowNum) -> new TableRef(rs.getString("table_schema"), rs.getString("table_name"))
        );

        for (TableRef ref : tableRefs) {
            SchemaMetadata meta = buildSchemaForTable(ref.schema, ref.name);
            if (meta != null) {
                result.add(meta);
            }
        }

        return result;
    }

    private SchemaMetadata buildSchemaForTable(String schema, String tableName) {
        String columnsSql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position;
            """;

        List<SchemaMetadata.ColumnMetadata> columns = jdbcTemplate.query(
            columnsSql,
            (rs, rowNum) -> new SchemaMetadata.ColumnMetadata(
                rs.getString("column_name"),
                rs.getString("data_type").toUpperCase(),
                "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                "Database column in " + tableName
            ),
            schema, tableName
        );

        String pkSql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = ? AND tc.table_name = ?;
            """;

        List<String> primaryKeys = jdbcTemplate.query(
            pkSql,
            (rs, rowNum) -> rs.getString("column_name"),
            schema, tableName
        );

        String fkSql = """
            SELECT 
              kcu1.column_name AS column_name,
              kcu2.table_name AS target_table,
              kcu2.column_name AS target_column
            FROM information_schema.referential_constraints rc
            JOIN information_schema.key_column_usage kcu1 
              ON rc.constraint_name = kcu1.constraint_name AND rc.constraint_schema = kcu1.constraint_schema
            JOIN information_schema.key_column_usage kcu2 
              ON rc.unique_constraint_name = kcu2.constraint_name AND rc.unique_constraint_schema = kcu2.constraint_schema
            WHERE kcu1.table_schema = ? AND kcu1.table_name = ?;
            """;

        List<SchemaMetadata.ForeignKeyMetadata> foreignKeys = jdbcTemplate.query(
            fkSql,
            (rs, rowNum) -> new SchemaMetadata.ForeignKeyMetadata(
                rs.getString("column_name"),
                rs.getString("target_table"),
                rs.getString("target_column")
            ),
            schema, tableName
        );

        boolean hasVector = columns.stream()
            .anyMatch(c -> c.dataType().toLowerCase().contains("vector")) 
            || tableName.equalsIgnoreCase("vector_chunks");

        String description = "Live Amazon RDS PostgreSQL table metadata for " + schema + "." + tableName;

        return new SchemaMetadata(
            schema,
            tableName,
            description,
            columns,
            primaryKeys,
            foreignKeys,
            hasVector
        );
    }

    private record TableRef(String schema, String name) {}
}
