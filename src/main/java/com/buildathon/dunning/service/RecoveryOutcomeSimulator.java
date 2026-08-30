package com.buildathon.dunning.service;

import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulates whether a retry attempt recovers the payment, since there's no
 * real payment gateway behind this project.
 * <p>
 * Base recovery probabilities are grounded in how each failure code actually
 * behaves in production dunning systems — e.g. GATEWAY_TIMEOUT is usually
 * transient and recovers well on any retry, EXPIRED_CARD barely recovers
 * without a payment-method-update prompt.
 * <p>
 * The strategy multiplier is what produces the AI-vs-naive lift: NAIVE gets
 * no adjustment (blind fixed schedule), AI gets a boost reflecting that it
 * times the retry window and channel around the specific failure reason.
 * The AI multiplier here is a placeholder until Day 3-4 wires the real
 * Gemini-driven strategy in — kept as a single tunable constant so nothing
 * else has to change when that lands.
 */
@Component
public class RecoveryOutcomeSimulator {

    private static final Map<FailureCode, Double> BASE_RECOVERY_PROBABILITY = new EnumMap<>(FailureCode.class);
    static {
        BASE_RECOVERY_PROBABILITY.put(FailureCode.INSUFFICIENT_FUNDS, 0.30);
        BASE_RECOVERY_PROBABILITY.put(FailureCode.BANK_DECLINE, 0.25);
        BASE_RECOVERY_PROBABILITY.put(FailureCode.EXPIRED_CARD, 0.10);
        BASE_RECOVERY_PROBABILITY.put(FailureCode.GATEWAY_TIMEOUT, 0.45);
    }

    private static final Map<Strategy, Double> STRATEGY_MULTIPLIER = new EnumMap<>(Strategy.class);
    static {
        STRATEGY_MULTIPLIER.put(Strategy.NAIVE, 1.0);
        STRATEGY_MULTIPLIER.put(Strategy.AI, 1.6);
    }

    private static final double DEFAULT_BASE_PROBABILITY = 0.20;
    private static final double MAX_PROBABILITY = 0.90;

    private final Random random = new Random();

    public RetryOutcome resolve(FailureCode failureCode, Strategy strategy) {
        double base = BASE_RECOVERY_PROBABILITY.getOrDefault(failureCode, DEFAULT_BASE_PROBABILITY);
        double multiplier = STRATEGY_MULTIPLIER.getOrDefault(strategy, 1.0);
        double probability = Math.min(base * multiplier, MAX_PROBABILITY);
        return random.nextDouble() < probability ? RetryOutcome.RECOVERED : RetryOutcome.FAILED;
    }
}
