package com.devcompass.ai.model;

import java.util.List;

public record SchemaMetadata(
    String tableSchema,
    String tableName,
    String description,
    List<ColumnMetadata> columns,
    List<String> primaryKeys,
    List<ForeignKeyMetadata> foreignKeys,
    boolean vectorIndexed
) {
    public record ColumnMetadata(
        String name,
        String dataType,
        boolean nullable,
        String description
    ) {}

    public record ForeignKeyMetadata(
        String columnName,
        String targetTable,
        String targetColumn
    ) {}
}
