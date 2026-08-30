package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;

import java.time.Instant;

public record TimelinePoint(
        Instant executedAt,
        Strategy strategy,
        RetryOutcome outcome
) {
    public static TimelinePoint from(RetryAttempt attempt) {
        return new TimelinePoint(attempt.getExecutedAt(), attempt.getStrategy(), attempt.getOutcome());
    }
}
