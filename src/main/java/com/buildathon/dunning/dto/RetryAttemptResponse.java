package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;

import java.time.Instant;

public record RetryAttemptResponse(
        Long id,
        Integer attemptNumber,
        Strategy strategy,
        Channel channel,
        Instant scheduledAt,
        Instant executedAt,
        RetryOutcome outcome,
        Integer aiRetryWindowHours,
        Double aiConfidence,
        String aiReasoning,
        Boolean aiSuggestAltPaymentMethod
) {
    public static RetryAttemptResponse from(RetryAttempt attempt) {
        return new RetryAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getStrategy(),
                attempt.getChannel(),
                attempt.getScheduledAt(),
                attempt.getExecutedAt(),
                attempt.getOutcome(),
                attempt.getAiRetryWindowHours(),
                attempt.getAiConfidence(),
                attempt.getAiReasoning(),
                attempt.getAiSuggestAltPaymentMethod()
        );
    }
}
