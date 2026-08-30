package com.buildathon.dunning.repository;

import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface RetryAttemptRepository extends JpaRepository<RetryAttempt, Long> {

    List<RetryAttempt> findByPayment_IdOrderByAttemptNumberAsc(Long paymentId);

    List<RetryAttempt> findByPayment_IdAndStrategyOrderByAttemptNumberAsc(Long paymentId, Strategy strategy);

    List<RetryAttempt> findByStrategyAndOutcomeAndScheduledAtLessThanEqual(
            Strategy strategy, RetryOutcome outcome, Instant scheduledBefore);

    // Used by the scheduler to pick up due attempts from every strategy in one pass
    List<RetryAttempt> findByOutcomeAndScheduledAtLessThanEqual(RetryOutcome outcome, Instant scheduledBefore);

    List<RetryAttempt> findByOutcome(RetryOutcome outcome);

    boolean existsByPayment_IdAndStrategyAndOutcome(Long paymentId, Strategy strategy, RetryOutcome outcome);

    // Used by the dashboard comparison endpoint
    List<RetryAttempt> findByStrategyAndOutcome(Strategy strategy, RetryOutcome outcome);

    // Used by the dashboard timeline endpoint
    List<RetryAttempt> findByExecutedAtIsNotNullOrderByExecutedAtAsc();
}
