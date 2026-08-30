package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.RecoveryMessageContent;
import com.buildathon.dunning.entity.Customer;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RecoveryMessage;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.entity.enums.Strategy;
import com.buildathon.dunning.repository.RecoveryMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Generates and records the recovery message for a retry attempt.
 * <p>
 * Only <b>AI</b>-strategy attempts on the <b>EMAIL</b> channel trigger a
 * real send via Resend — that's the one track meant to reflect what would
 * actually run in production, and the one worth spending real email quota
 * on. Everything else (the NAIVE shadow track, and any SMS attempt, since
 * there's no free real-SMS provider wired up) still generates and stores
 * message content — for dashboard/comparison purposes — but is recorded as
 * "simulated" rather than actually sent.
 */
@Service
public class RecoveryMessageService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryMessageService.class);

    private final MessageGenerationService messageGenerationService;
    private final ResendEmailService resendEmailService;
    private final RecoveryMessageRepository recoveryMessageRepository;

    public RecoveryMessageService(MessageGenerationService messageGenerationService,
                                   ResendEmailService resendEmailService,
                                   RecoveryMessageRepository recoveryMessageRepository) {
        this.messageGenerationService = messageGenerationService;
        this.resendEmailService = resendEmailService;
        this.recoveryMessageRepository = recoveryMessageRepository;
    }

    public void sendForAttempt(RetryAttempt attempt, FailureCode failureCode) {
        Payment payment = attempt.getPayment();
        Customer customer = payment.getCustomer();
        boolean suggestAlt = Boolean.TRUE.equals(attempt.getAiSuggestAltPaymentMethod());

        RecoveryMessageContent content = messageGenerationService.generate(
                failureCode, payment.getAmount(), payment.getCurrency(), customer.getName(), suggestAlt);

        boolean shouldSendReal = attempt.getStrategy() == Strategy.AI && attempt.getChannel() == Channel.EMAIL;
        String provider = "simulated";

        if (shouldSendReal) {
            try {
                resendEmailService.send(customer.getEmail(), content.subject(), content.body());
                provider = "resend";
            } catch (Exception e) {
                log.warn("Resend send failed ({}) — recording message as simulated for attempt {}",
                        e.getMessage(), attempt.getId());
            }
        }

        RecoveryMessage message = RecoveryMessage.builder()
                .retryAttempt(attempt)
                .channel(attempt.getChannel())
                .subject(content.subject())
                .body(content.body())
                .sentAt(Instant.now())
                .provider(provider)
                .build();
        recoveryMessageRepository.save(message);
    }
}
