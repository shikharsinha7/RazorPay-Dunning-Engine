package com.buildathon.dunning.service;

import com.buildathon.dunning.entity.FailureEvent;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.entity.enums.PaymentStatus;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;
import com.buildathon.dunning.repository.FailureEventRepository;
import com.buildathon.dunning.repository.PaymentRepository;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Executes a single retry attempt: resolves whether it recovers the payment
 * (via the simulator, since there's no real gateway), sends/simulates its
 * recovery message, and cascades the effect within that attempt's own
 * strategy track — skipping later attempts once one succeeds, marking the
 * payment lost once every attempt in a strategy's schedule has failed.
 * <p>
 * Both AI and NAIVE tracks run and resolve independently for every payment,
 * but only the <b>AI</b> strategy is allowed to update {@code payment.status}.
 * AI is what would actually run in production; NAIVE exists purely as a
 * shadow simulation so the dashboard can compute a recovery-rate comparison
 * between the two — it should never affect the payment's real outcome.
 */
@Service
public class RetryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutionService.class);

    private final RetryAttemptRepository retryAttemptRepository;
    private final PaymentRepository paymentRepository;
    private final FailureEventRepository failureEventRepository;
    private final RecoveryOutcomeSimulator outcomeSimulator;
    private final RecoveryMessageService recoveryMessageService;

    public RetryExecutionService(RetryAttemptRepository retryAttemptRepository,
                                  PaymentRepository paymentRepository,
                                  FailureEventRepository failureEventRepository,
                                  RecoveryOutcomeSimulator outcomeSimulator,
                                  RecoveryMessageService recoveryMessageService) {
        this.retryAttemptRepository = retryAttemptRepository;
        this.paymentRepository = paymentRepository;
        this.failureEventRepository = failureEventRepository;
        this.outcomeSimulator = outcomeSimulator;
        this.recoveryMessageService = recoveryMessageService;
    }

    @Transactional
    public void execute(RetryAttempt attempt) {
        Payment payment = attempt.getPayment();
        Strategy strategy = attempt.getStrategy();

        // An earlier attempt in THIS strategy already recovered the payment —
        // skip. (Not payment.status, since that now only reflects AI.)
        if (retryAttemptRepository.existsByPayment_IdAndStrategyAndOutcome(
                payment.getId(), strategy, RetryOutcome.RECOVERED)) {
            markSkipped(attempt);
            return;
        }

        FailureCode failureCode = failureEventRepository.findFirstByPayment_Id(payment.getId())
                .map(FailureEvent::getFailureCode)
                .orElse(null);

        RetryOutcome outcome = outcomeSimulator.resolve(failureCode, strategy);
        attempt.setOutcome(outcome);
        attempt.setExecutedAt(Instant.now());
        retryAttemptRepository.save(attempt);

        log.info("Executed {} attempt #{} for payment {} ({}) -> {}",
                strategy, attempt.getAttemptNumber(), payment.getId(), failureCode, outcome);

        recoveryMessageService.sendForAttempt(attempt, failureCode);

        if (outcome == RetryOutcome.RECOVERED) {
            skipRemainingAttempts(payment, strategy);
            if (strategy == Strategy.AI) {
                payment.setStatus(PaymentStatus.RECOVERED);
                paymentRepository.save(payment);
            }
        } else if (strategy == Strategy.AI) {
            evaluateWhetherPaymentIsLost(payment);
        }
        // NAIVE failures never touch payment.status — it's a shadow track.
    }

    private void markSkipped(RetryAttempt attempt) {
        attempt.setOutcome(RetryOutcome.SKIPPED);
        attempt.setExecutedAt(Instant.now());
        retryAttemptRepository.save(attempt);
    }

    private void skipRemainingAttempts(Payment payment, Strategy strategy) {
        retryAttemptRepository.findByPayment_IdAndStrategyOrderByAttemptNumberAsc(payment.getId(), strategy)
                .stream()
                .filter(a -> a.getOutcome() == RetryOutcome.PENDING)
                .forEach(this::markSkipped);
    }

    private void evaluateWhetherPaymentIsLost(Payment payment) {
        List<RetryAttempt> aiAttempts = retryAttemptRepository
                .findByPayment_IdAndStrategyOrderByAttemptNumberAsc(payment.getId(), Strategy.AI);
        boolean anyPending = aiAttempts.stream().anyMatch(a -> a.getOutcome() == RetryOutcome.PENDING);
        boolean noneRecovered = aiAttempts.stream().noneMatch(a -> a.getOutcome() == RetryOutcome.RECOVERED);
        if (!anyPending && noneRecovered && payment.getStatus() != PaymentStatus.RECOVERED) {
            payment.setStatus(PaymentStatus.LOST);
            paymentRepository.save(payment);
        }
    }
}
