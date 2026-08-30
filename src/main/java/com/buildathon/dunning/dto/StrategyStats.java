package com.buildathon.dunning.dto;

public record StrategyStats(
        long totalPayments,
        long recoveredPayments,
        double recoveryRatePercent
) {
}
