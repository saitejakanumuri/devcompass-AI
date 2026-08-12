package com.devcompass.ai.service;

import com.devcompass.ai.model.AccountKnowledgeConfig;
import com.devcompass.ai.model.SchemaMetadata;
import com.devcompass.ai.model.SourceType;
import com.devcompass.ai.repository.AccountConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaExplorerService {

    private static final Logger log = LoggerFactory.getLogger(SchemaExplorerService.class);

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountConfigRepository accountConfigRepository;

    private static final Set<String> IGNORED_SYSTEM_SCHEMAS = Set.of("pg_catalog", "information_schema", "pgvector");
    private static final Set<String> INTERNAL_APP_TABLES = Set.of(
        "vector_chunks", "accounts", "users", "account_knowledge_configs", "user_knowledge_configs",
        "knowledge_sync_tracker", "notion_webhook_queue"
    );

    public boolean testConnection(String rawUrl, String rawUsername, String rawPassword) {
        NormalizedDbConfig norm = normalizeDbConfig(rawUrl, rawUsername, rawPassword);
        if (norm.url().isBlank()) return false;
        try {
            DriverManagerDataSource ds = new DriverManagerDataSource(norm.url(), norm.username(), norm.password());
            ds.setDriverClassName("org.postgresql.Driver");
            JdbcTemplate testJdbc = new JdbcTemplate(ds);
            Integer val = testJdbc.queryForObject("SELECT 1", Integer.class);
            return val != null && val == 1;
        } catch (Exception e) {
            log.error("[SchemaExplorerService] Failed to test database connection for URL '{}': {}", norm.url(), e.getMessage());
            return false;
        }
    }

    public List<SchemaMetadata> getAllSchemas() {
        return getSchemasForAccount(null, null);
    }

    public List<SchemaMetadata> getSchemasForAccount(UUID accountId, UUID userId) {
        // Try account target database connection if configured
        if (accountId != null && accountConfigRepository != null) {
            Optional<AccountKnowledgeConfig> dbConfigOpt = accountConfigRepository.getConfigForAccountUserAndType(accountId, userId, SourceType.DATABASE_METADATA);
            if (dbConfigOpt.isPresent()) {
                Map<String, Object> cfgMap = dbConfigOpt.get().configJson();
                String rawUrl = cfgMap.getOrDefault("url", cfgMap.getOrDefault("dbUrl", "")).toString();
                String rawUsername = cfgMap.getOrDefault("username", cfgMap.getOrDefault("dbUsername", "")).toString();
                String rawPassword = cfgMap.getOrDefault("password", cfgMap.getOrDefault("dbPassword", "")).toString();

                NormalizedDbConfig norm = normalizeDbConfig(rawUrl, rawUsername, rawPassword);

                if (!norm.url().isBlank() && testConnection(norm.url(), norm.username(), norm.password())) {
                    try {
                        DriverManagerDataSource ds = new DriverManagerDataSource(norm.url(), norm.username(), norm.password());
                        ds.setDriverClassName("org.postgresql.Driver");
                        JdbcTemplate targetJdbc = new JdbcTemplate(ds);
                        List<SchemaMetadata> accountSchemas = fetchLiveSchemasFromJdbc(targetJdbc, false);
                        if (!accountSchemas.isEmpty()) {
                            log.info("[SchemaExplorerService] Successfully fetched {} tables from configured account target DB", accountSchemas.size());
                            return accountSchemas;
                        }
                    } catch (Exception e) {
                        log.warn("[SchemaExplorerService] Failed querying account target database schema: {}", e.getMessage());
                    }
                }
            }
        }

        // Fallback to default system JDBC, filtering out pgvector and system schemas
        if (jdbcTemplate != null) {
            try {
                List<SchemaMetadata> schemas = fetchLiveSchemasFromJdbc(jdbcTemplate, true);
                if (!schemas.isEmpty()) {
                    return schemas;
                }
                // If filtering internal tables leaves 0 tables, return all live database tables
                return fetchLiveSchemasFromJdbc(jdbcTemplate, false);
            } catch (Exception e) {
                log.warn("[SchemaExplorerService] Failed to query default database schema: {}", e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    public Optional<SchemaMetadata> getSchemaForTable(String tableName, UUID accountId, UUID userId) {
        return getSchemasForAccount(accountId, userId).stream()
            .filter(s -> s.tableName().equalsIgnoreCase(tableName))
            .findFirst();
    }

    private List<SchemaMetadata> fetchLiveSchemasFromJdbc(JdbcTemplate targetJdbc, boolean filterInternalAppTables) {
        String tablesSql = """
            SELECT table_schema, table_name
            FROM information_schema.tables
            WHERE table_schema NOT IN ('pg_catalog', 'information_schema', 'pgvector')
              AND table_type = 'BASE TABLE'
            ORDER BY table_name;
            """;

        List<SchemaMetadata> result = new ArrayList<>();

        List<TableRef> tableRefs = targetJdbc.query(
            tablesSql,
            (rs, rowNum) -> new TableRef(rs.getString("table_schema"), rs.getString("table_name"))
        );

        for (TableRef ref : tableRefs) {
            if (IGNORED_SYSTEM_SCHEMAS.contains(ref.schema.toLowerCase())) {
                continue;
            }
            if (filterInternalAppTables && INTERNAL_APP_TABLES.contains(ref.name.toLowerCase())) {
                continue;
            }

            SchemaMetadata meta = buildSchemaForTable(targetJdbc, ref.schema, ref.name);
            if (meta != null) {
                result.add(meta);
            }
        }

        return result;
    }

    private SchemaMetadata buildSchemaForTable(JdbcTemplate targetJdbc, String schema, String tableName) {
        String columnsSql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position;
            """;

        List<SchemaMetadata.ColumnMetadata> columns = targetJdbc.query(
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

        List<String> primaryKeys = targetJdbc.query(
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

        List<SchemaMetadata.ForeignKeyMetadata> foreignKeys = targetJdbc.query(
            fkSql,
            (rs, rowNum) -> new SchemaMetadata.ForeignKeyMetadata(
                rs.getString("column_name"),
                rs.getString("target_table"),
                rs.getString("target_column")
            ),
            schema, tableName
        );

        boolean hasVector = columns.stream()
            .anyMatch(c -> c.dataType().toLowerCase().contains("vector"));

        String description = "Relational PostgreSQL database metadata for " + schema + "." + tableName;

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

    private NormalizedDbConfig normalizeDbConfig(String rawUrl, String rawUser, String rawPass) {
        String url = rawUrl != null ? rawUrl.trim() : "";
        String user = rawUser != null ? rawUser.trim() : "";
        String pass = rawPass != null ? rawPass.trim() : "";

        if (url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }

        if (url.startsWith("jdbc:postgresql://") && url.contains("@")) {
            try {
                String userInfoAndHost = url.substring("jdbc:postgresql://".length());
                int atIndex = userInfoAndHost.indexOf("@");
                if (atIndex > 0) {
                    String userInfo = userInfoAndHost.substring(0, atIndex);
                    String hostAndDb = userInfoAndHost.substring(atIndex + 1);
                    url = "jdbc:postgresql://" + hostAndDb;
                    if (userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        if (user.isBlank()) user = parts[0];
                        if (pass.isBlank()) pass = parts[1];
                    } else if (user.isBlank()) {
                        user = userInfo;
                    }
                }
            } catch (Exception ignored) {}
        }

        return new NormalizedDbConfig(url, user, pass);
    }

    private record TableRef(String schema, String name) {}
    private record NormalizedDbConfig(String url, String username, String password) {}
}
