package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.FailureClassification;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.FailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Wraps {@link GeminiClient} with a deterministic rule-based fallback per
 * failure code. A missing/invalid API key, quota limit, or network blip
 * during a live demo degrades gracefully to these rules instead of breaking
 * ingest — the fallback values mirror the same reasoning Gemini is prompted
 * with, just hardcoded.
 */
@Service
public class FailureClassificationService {

    private static final Logger log = LoggerFactory.getLogger(FailureClassificationService.class);

    private static final Map<FailureCode, FailureClassification> FALLBACKS = new EnumMap<>(FailureCode.class);
    static {
        FALLBACKS.put(FailureCode.INSUFFICIENT_FUNDS,
                new FailureClassification(48, Channel.EMAIL, false, 0.5,
                        "Fallback: insufficient funds often resolve after a short wait, commonly near payday."));
        FALLBACKS.put(FailureCode.EXPIRED_CARD,
                new FailureClassification(2, Channel.EMAIL, true, 0.5,
                        "Fallback: an expired card rarely recovers without prompting for an updated payment method."));
        FALLBACKS.put(FailureCode.BANK_DECLINE,
                new FailureClassification(24, Channel.SMS, false, 0.5,
                        "Fallback: a bank decline often needs about a day, with an SMS nudge to the customer."));
        FALLBACKS.put(FailureCode.GATEWAY_TIMEOUT,
                new FailureClassification(1, Channel.EMAIL, false, 0.5,
                        "Fallback: gateway timeouts are usually transient and often resolve on a quick retry."));
    }

    private static final FailureClassification DEFAULT_FALLBACK =
            new FailureClassification(24, Channel.EMAIL, false, 0.4, "Fallback: default retry strategy.");

    private final GeminiClient geminiClient;

    public FailureClassificationService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public FailureClassification classify(FailureCode failureCode, BigDecimal amount, String currency) {
        try {
            return geminiClient.classify(failureCode, amount, currency);
        } catch (Exception e) {
            log.warn("Gemini classification failed ({}) — using rule-based fallback for {}",
                    e.getMessage(), failureCode);
            return FALLBACKS.getOrDefault(failureCode, DEFAULT_FALLBACK);
        }
    }
}
