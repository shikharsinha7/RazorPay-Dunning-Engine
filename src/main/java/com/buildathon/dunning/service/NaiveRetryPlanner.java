package com.buildathon.dunning.service;

import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.Strategy;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Builds the naive baseline retry schedule: a blind, fixed calendar of
 * retries regardless of failure reason — 24h, then +72h, then +7d from the
 * original failure. This is the thing the AI strategy (Day 3-4) is designed
 * to beat on the dashboard.
 */
@Component
public class NaiveRetryPlanner {

    // Cumulative hours from the original failure: 24h, then 96h (24+72), then 264h (96+168)
    private static final int[] CUMULATIVE_OFFSET_HOURS = {24, 96, 264};

    private final RetryAttemptRepository retryAttemptRepository;

    public NaiveRetryPlanner(RetryAttemptRepository retryAttemptRepository) {
        this.retryAttemptRepository = retryAttemptRepository;
    }

    public List<RetryAttempt> scheduleNaiveRetries(Payment payment, Instant failureOccurredAt) {
        return IntStream.range(0, CUMULATIVE_OFFSET_HOURS.length)
                .mapToObj(i -> retryAttemptRepository.save(
                        RetryAttempt.builder()
                                .payment(payment)
                                .attemptNumber(i + 1)
                                .strategy(Strategy.NAIVE)
                                .scheduledAt(failureOccurredAt.plus(CUMULATIVE_OFFSET_HOURS[i], ChronoUnit.HOURS))
                                .channel(Channel.EMAIL)
                                .build()))
                .toList();
    }
}
