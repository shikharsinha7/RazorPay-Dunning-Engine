package com.buildathon.dunning.scheduler;

import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import com.buildathon.dunning.service.RetryExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Polls for retry attempts — from either the AI or NAIVE track — whose
 * scheduled time has arrived, and executes them. Runs every minute; actual
 * retries are scheduled hours or days out, so this cadence only controls how
 * promptly a due one gets noticed. For demoing without waiting real days,
 * use POST /api/admin/retries/force-run instead.
 */
@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final RetryAttemptRepository retryAttemptRepository;
    private final RetryExecutionService retryExecutionService;

    public RetryScheduler(RetryAttemptRepository retryAttemptRepository,
                           RetryExecutionService retryExecutionService) {
        this.retryAttemptRepository = retryAttemptRepository;
        this.retryExecutionService = retryExecutionService;
    }

    @Scheduled(fixedRate = 60_000)
    public void runDueRetries() {
        List<RetryAttempt> due = retryAttemptRepository
                .findByOutcomeAndScheduledAtLessThanEqual(RetryOutcome.PENDING, Instant.now());
        if (due.isEmpty()) {
            return;
        }
        log.info("Found {} due retry attempt(s) across both strategies", due.size());
        due.forEach(retryExecutionService::execute);
    }
}
