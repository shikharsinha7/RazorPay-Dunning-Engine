package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.ComparisonResponse;
import com.buildathon.dunning.dto.FunnelResponse;
import com.buildathon.dunning.dto.StrategyStats;
import com.buildathon.dunning.dto.TimelinePoint;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.PaymentStatus;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;
import com.buildathon.dunning.repository.PaymentRepository;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the dashboard's headline numbers directly from existing
 * Payment/RetryAttempt data — no new state, purely read-side aggregation.
 */
@Service
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final RetryAttemptRepository retryAttemptRepository;

    public DashboardService(PaymentRepository paymentRepository, RetryAttemptRepository retryAttemptRepository) {
        this.paymentRepository = paymentRepository;
        this.retryAttemptRepository = retryAttemptRepository;
    }

    public FunnelResponse getFunnel() {
        long total = paymentRepository.count();
        long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long recovered = paymentRepository.countByStatus(PaymentStatus.RECOVERED);
        long lost = paymentRepository.countByStatus(PaymentStatus.LOST);
        return new FunnelResponse(total, failed, recovered, lost);
    }

    public ComparisonResponse getComparison() {
        long totalPayments = paymentRepository.count();
        StrategyStats ai = strategyStats(Strategy.AI, totalPayments);
        StrategyStats naive = strategyStats(Strategy.NAIVE, totalPayments);
        double lift = ai.recoveryRatePercent() - naive.recoveryRatePercent();
        return new ComparisonResponse(ai, naive, lift);
    }

    public List<TimelinePoint> getTimeline() {
        return retryAttemptRepository.findByExecutedAtIsNotNullOrderByExecutedAtAsc()
                .stream()
                .map(TimelinePoint::from)
                .toList();
    }

    private StrategyStats strategyStats(Strategy strategy, long totalPayments) {
        List<RetryAttempt> recoveredAttempts = retryAttemptRepository
                .findByStrategyAndOutcome(strategy, RetryOutcome.RECOVERED);
        Set<Long> recoveredPaymentIds = recoveredAttempts.stream()
                .map(a -> a.getPayment().getId())
                .collect(Collectors.toSet());
        long recoveredCount = recoveredPaymentIds.size();
        double rate = totalPayments == 0 ? 0.0 : (recoveredCount * 100.0) / totalPayments;
        return new StrategyStats(totalPayments, recoveredCount, round2(rate));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
