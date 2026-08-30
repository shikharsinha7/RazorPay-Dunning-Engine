package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.enums.Channel;

/**
 * The AI's (or fallback's) recommendation for how to retry a failed payment.
 */
public record FailureClassification(
        int retryWindowHours,
        Channel channel,
        boolean suggestAltPaymentMethod,
        double confidence,
        String reasoning
) {
}
