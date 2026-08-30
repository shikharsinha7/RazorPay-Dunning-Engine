package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.FailureClassification;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.Strategy;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the AI-driven retry schedule: 3 attempts using exponential backoff
 * around the AI-recommended window (with jitter, so timing isn't perfectly
 * predictable), and channel diversity — starting on the AI's recommended
 * channel and diversifying on later attempts rather than the naive track's
 * fixed all-EMAIL schedule.
 */
@Component
public class AiRetryPlanner {

    private static final double JITTER_FRACTION = 0.15; // +/- 15%
    private static final long MAX_GAP_HOURS = 30L * 24; // don't let backoff run past 30 days

    private final RetryAttemptRepository retryAttemptRepository;
    private final Random random = new Random();

    public AiRetryPlanner(RetryAttemptRepository retryAttemptRepository) {
        this.retryAttemptRepository = retryAttemptRepository;
    }

    public List<RetryAttempt> scheduleAiRetries(Payment payment, Instant failureOccurredAt,
                                                 FailureClassification classification) {
        long baseHours = Math.max(1, classification.retryWindowHours());

        long gap1 = applyJitter(baseHours);
        long gap2 = applyJitter(baseHours * 2);
        long gap3 = applyJitter(baseHours * 4);

        Instant t1 = failureOccurredAt.plus(gap1, ChronoUnit.HOURS);
        Instant t2 = t1.plus(gap2, ChronoUnit.HOURS);
        Instant t3 = t2.plus(gap3, ChronoUnit.HOURS);

        Channel primaryChannel = classification.channel();
        Channel secondaryChannel = primaryChannel == Channel.EMAIL ? Channel.SMS : Channel.EMAIL;

        List<RetryAttempt> attempts = new ArrayList<>();
        attempts.add(buildAttempt(payment, 1, t1, primaryChannel, classification));
        attempts.add(buildAttempt(payment, 2, t2, secondaryChannel, classification));
        attempts.add(buildAttempt(payment, 3, t3, Channel.EMAIL, classification));

        return retryAttemptRepository.saveAll(attempts);
    }

    private RetryAttempt buildAttempt(Payment payment, int attemptNumber, Instant scheduledAt,
                                       Channel channel, FailureClassification classification) {
        return RetryAttempt.builder()
                .payment(payment)
                .attemptNumber(attemptNumber)
                .strategy(Strategy.AI)
                .scheduledAt(scheduledAt)
                .channel(channel)
                .aiRetryWindowHours(classification.retryWindowHours())
                .aiConfidence(classification.confidence())
                .aiReasoning(classification.reasoning())
                .aiSuggestAltPaymentMethod(classification.suggestAltPaymentMethod())
                .build();
    }

    private long applyJitter(long hours) {
        long capped = Math.min(hours, MAX_GAP_HOURS);
        double jitter = (random.nextDouble() * 2 - 1) * JITTER_FRACTION; // -0.15 .. +0.15
        return Math.max(1, Math.round(capped * (1 + jitter)));
    }
}
