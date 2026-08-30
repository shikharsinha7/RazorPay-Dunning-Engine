package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.RecoveryMessageContent;
import com.buildathon.dunning.entity.enums.FailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Wraps {@link GeminiClient#generateMessage} with a template-based fallback,
 * for the same reason {@link FailureClassificationService} has one: a
 * Gemini outage during the demo should degrade gracefully, not break the
 * retry flow.
 */
@Service
public class MessageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MessageGenerationService.class);

    private final GeminiClient geminiClient;

    public MessageGenerationService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public RecoveryMessageContent generate(FailureCode failureCode, BigDecimal amount, String currency,
                                            String customerName, boolean suggestAltPaymentMethod) {
        try {
            return geminiClient.generateMessage(failureCode, amount, currency, customerName, suggestAltPaymentMethod);
        } catch (Exception e) {
            log.warn("Gemini message generation failed ({}) — using fallback template for {}",
                    e.getMessage(), failureCode);
            return fallbackTemplate(amount, currency, suggestAltPaymentMethod);
        }
    }

    private RecoveryMessageContent fallbackTemplate(BigDecimal amount, String currency,
                                                      boolean suggestAltPaymentMethod) {
        if (suggestAltPaymentMethod) {
            return new RecoveryMessageContent(
                    "Action needed: update your payment method",
                    "We weren't able to process your payment of " + amount + " " + currency
                            + ". Please update your payment method to avoid any interruption to your subscription.");
        }
        return new RecoveryMessageContent(
                "We'll retry your recent payment",
                "Your payment of " + amount + " " + currency + " didn't go through, but no action is needed —"
                        + " we'll automatically retry it shortly.");
    }
}
