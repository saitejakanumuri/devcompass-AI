package com.devcompass.ai.controller;

import com.devcompass.ai.model.SchemaMetadata;
import com.devcompass.ai.service.SchemaExplorerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schema")
public class SchemaExplorerController {

    private final SchemaExplorerService schemaExplorerService;

    public SchemaExplorerController(SchemaExplorerService schemaExplorerService) {
        this.schemaExplorerService = schemaExplorerService;
    }

    @GetMapping("/tables")
    public ResponseEntity<List<SchemaMetadata>> getAllTableSchemas() {
        return ResponseEntity.ok(schemaExplorerService.getAllSchemas());
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<SchemaMetadata> getTableSchema(@PathVariable String tableName) {
        return schemaExplorerService.getSchemaForTable(tableName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
