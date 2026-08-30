package com.buildathon.dunning.controller;

import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import com.buildathon.dunning.service.RetryExecutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Demo-only helper: force-executes every PENDING retry attempt immediately,
 * ignoring scheduledAt. Nobody's waiting 7 real days during a judge demo —
 * the real scheduler (NaiveRetryScheduler) is what respects actual timing.
 */
@RestController
@RequestMapping("/api/admin/retries")
public class RetryAdminController {

    private final RetryAttemptRepository retryAttemptRepository;
    private final RetryExecutionService retryExecutionService;

    public RetryAdminController(RetryAttemptRepository retryAttemptRepository,
                                 RetryExecutionService retryExecutionService) {
        this.retryAttemptRepository = retryAttemptRepository;
        this.retryExecutionService = retryExecutionService;
    }

    @PostMapping("/force-run")
    public Map<String, Object> forceRunAllPending() {
        List<RetryAttempt> pending = retryAttemptRepository.findByOutcome(RetryOutcome.PENDING).stream()
                .sorted(Comparator.comparing((RetryAttempt a) -> a.getPayment().getId())
                        .thenComparing(RetryAttempt::getAttemptNumber))
                .toList();

        int executed = 0;
        for (RetryAttempt attempt : pending) {
            // Re-fetch in case an earlier attempt processed in this same loop
            // already skipped this one (e.g. its payment just got recovered).
            RetryAttempt fresh = retryAttemptRepository.findById(attempt.getId()).orElse(null);
            if (fresh != null && fresh.getOutcome() == RetryOutcome.PENDING) {
                retryExecutionService.execute(fresh);
                executed++;
            }
        }
        return Map.of("executedCount", executed);
    }
}
