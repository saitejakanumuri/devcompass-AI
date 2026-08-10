package com.devcompass.ai.controller;

import com.devcompass.ai.repository.NotionWebhookQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/notion")
public class NotionWebhookController {

    private static final Logger log = LoggerFactory.getLogger(NotionWebhookController.class);

    private final NotionWebhookQueueRepository queueRepository;

    @Autowired
    public NotionWebhookController(NotionWebhookQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleNotionWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received Notion webhook payload: {}", payload);

        // Handle URL verification handshake if required by Notion/Webhook provider
        if (payload.containsKey("challenge")) {
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }
        if (payload.containsKey("verification_token")) {
            return ResponseEntity.ok(Map.of("verification_token", payload.get("verification_token")));
        }

        String pageId = extractPageId(payload);
        String eventType = payload.containsKey("type") ? payload.get("type").toString() : "page_updated";

        if (pageId != null && !pageId.isBlank()) {
            String taskId = "task-" + UUID.randomUUID().toString();
            queueRepository.enqueueTask(taskId, pageId, eventType);
            log.info("Enqueued Notion webhook page sync task {} for page_id: {}", taskId, pageId);

            return ResponseEntity.ok(Map.of(
                "status", "QUEUED",
                "taskId", taskId,
                "pageId", pageId,
                "eventType", eventType
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
            "status", "IGNORED",
            "message", "No valid page_id found in webhook payload"
        ));
    }

    private String extractPageId(Map<String, Object> payload) {
        // Notion Webhook formats can send page ID under entity.id, data.id, page_id, or id
        if (payload.containsKey("page_id")) {
            return payload.get("page_id").toString();
        }
        if (payload.get("entity") instanceof Map entityMap && entityMap.containsKey("id")) {
            return entityMap.get("id").toString();
        }
        if (payload.get("data") instanceof Map dataMap && dataMap.containsKey("id")) {
            return dataMap.get("id").toString();
        }
        if (payload.containsKey("id")) {
            return payload.get("id").toString();
        }
        return null;
    }
}
