package com.buildathon.dunning.dto;

public record ComparisonResponse(
        StrategyStats ai,
        StrategyStats naive,
        double liftPercentagePoints
) {
}
