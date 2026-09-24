package com.devcompass.ai.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * Background scheduler for async knowledge source processing.
 * Currently a placeholder — sync is triggered on-demand from the Sources controller.
 * Extend this with @Scheduled methods if periodic re-sync is needed.
 */
@Component
@EnableScheduling
@EnableAsync
public class AsyncIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncIngestionScheduler.class);

    // Future: add @Scheduled methods for periodic re-sync per user
}
