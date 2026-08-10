package com.devcompass.ai.scheduler;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SyncTrackerRecord;
import com.devcompass.ai.pipeline.IngestionPipelineService;
import com.devcompass.ai.repository.NotionWebhookQueueRepository;
import com.devcompass.ai.repository.SyncTrackerRepository;
import com.devcompass.ai.source.KnowledgeSource;
import com.devcompass.ai.source.KnowledgeSourceRegistry;
import com.devcompass.ai.source.NotionKnowledgeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@EnableAsync
public class AsyncIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncIngestionScheduler.class);

    private final KnowledgeSourceRegistry sourceRegistry;
    private final SyncTrackerRepository trackerRepository;
    private final IngestionPipelineService ingestionPipelineService;
    private final NotionWebhookQueueRepository webhookQueueRepository;
    private final NotionKnowledgeSource notionKnowledgeSource;

    @Autowired
    public AsyncIngestionScheduler(
        KnowledgeSourceRegistry sourceRegistry,
        SyncTrackerRepository trackerRepository,
        IngestionPipelineService ingestionPipelineService,
        NotionWebhookQueueRepository webhookQueueRepository,
        NotionKnowledgeSource notionKnowledgeSource
    ) {
        this.sourceRegistry = sourceRegistry;
        this.trackerRepository = trackerRepository;
        this.ingestionPipelineService = ingestionPipelineService;
        this.webhookQueueRepository = webhookQueueRepository;
        this.notionKnowledgeSource = notionKnowledgeSource;
    }

    // Runs asynchronously every 15 seconds to process pending Notion webhook tasks
    @Scheduled(fixedDelay = 15000)
    @Async
    public void processPendingNotionWebhookTasks() {
        List<NotionWebhookQueueRepository.WebhookTask> pendingTasks = webhookQueueRepository.fetchPendingTasks(5);
        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("[WebhookScheduler] Found {} pending Notion webhook tasks to process...", pendingTasks.size());

        for (NotionWebhookQueueRepository.WebhookTask task : pendingTasks) {
            try {
                log.info("[WebhookScheduler] Processing webhook task {} for page_id: {}", task.id(), task.pageId());
                webhookQueueRepository.updateTaskStatus(task.id(), "PROCESSING", null);

                Document document = notionKnowledgeSource.fetchSinglePageDocument(task.pageId());
                if (document != null) {
                    int chunkCount = ingestionPipelineService.ingestSingleDocument(document);

                    Instant lastEditedTime = parseInstant(document.metadata().get("lastEditedTime"));
                    trackerRepository.upsertSyncRecord(new SyncTrackerRecord(
                        document.id(),
                        document.title(),
                        document.sourceType(),
                        document.sourceId(),
                        lastEditedTime != null ? lastEditedTime : Instant.now(),
                        Instant.now(),
                        chunkCount,
                        "SYNCED"
                    ));

                    webhookQueueRepository.updateTaskStatus(task.id(), "COMPLETED", null);
                    log.info("[WebhookScheduler] Successfully updated embeddings for page_id {} ({} chunks created)", task.pageId(), chunkCount);
                } else {
                    webhookQueueRepository.updateTaskStatus(task.id(), "FAILED", "Document content empty or fetch failed");
                    log.warn("[WebhookScheduler] Task {} failed: Could not fetch Notion document for page_id: {}", task.id(), task.pageId());
                }
            } catch (Exception e) {
                log.error("[WebhookScheduler] Error processing webhook task {}: {}", task.id(), e.getMessage());
                webhookQueueRepository.updateTaskStatus(task.id(), "FAILED", e.getMessage());
            }
        }
    }

    // Runs asynchronously every 5 minutes in background for full incremental sync
    @Scheduled(fixedRate = 300000)
    @Async
    public void runIncrementalNotionBackgroundSync() {
        log.info("[AsyncScheduler] Starting background incremental knowledge sync check...");

        for (KnowledgeSource source : sourceRegistry.getAllSources()) {
            try {
                List<Document> docs = source.sync();
                for (Document doc : docs) {
                    Instant lastEditedTime = parseInstant(doc.metadata().get("lastEditedTime"));
                    boolean needsEmbedding = trackerRepository.shouldReembed(doc.sourceId(), lastEditedTime);

                    if (needsEmbedding) {
                        log.info("[AsyncScheduler] Document '{}' (ID: {}) was modified at {}. Re-embedding into pgvector...", 
                            doc.title(), doc.sourceId(), lastEditedTime);

                        // Trigger ingestion for updated document
                        ingestionPipelineService.ingestSourceType(doc.sourceType());

                        // Update tracking record in knowledge_sync_tracker PostgreSQL table
                        trackerRepository.upsertSyncRecord(new SyncTrackerRecord(
                            doc.id(),
                            doc.title(),
                            doc.sourceType(),
                            doc.sourceId(),
                            lastEditedTime != null ? lastEditedTime : Instant.now(),
                            Instant.now(),
                            1,
                            "SYNCED"
                        ));
                    } else {
                        log.info("[AsyncScheduler] Document '{}' (ID: {}) is up-to-date. SKIPPING re-embedding.", 
                            doc.title(), doc.sourceId());
                    }
                }
            } catch (Exception e) {
                log.error("[AsyncScheduler] Error during incremental sync for source '{}': {}", source.sourceName(), e.getMessage());
            }
        }
    }

    private Instant parseInstant(Object val) {
        if (val == null) return null;
        try {
            return Instant.parse(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
