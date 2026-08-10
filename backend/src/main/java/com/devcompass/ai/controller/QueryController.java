package com.devcompass.ai.controller;

import com.devcompass.ai.model.QueryRequest;
import com.devcompass.ai.model.QueryResponse;
import com.devcompass.ai.service.QueryEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryEngineService queryEngineService;

    public QueryController(QueryEngineService queryEngineService) {
        this.queryEngineService = queryEngineService;
    }

    @PostMapping
    public ResponseEntity<QueryResponse> querySystem(@RequestBody QueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        QueryResponse response = queryEngineService.executeQuery(request);
        return ResponseEntity.ok(response);
    }
}
