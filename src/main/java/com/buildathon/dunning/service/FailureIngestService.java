package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.FailureClassification;
import com.buildathon.dunning.dto.FailureIngestRequest;
import com.buildathon.dunning.entity.Customer;
import com.buildathon.dunning.entity.FailureEvent;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.PaymentStatus;
import com.buildathon.dunning.repository.CustomerRepository;
import com.buildathon.dunning.repository.FailureEventRepository;
import com.buildathon.dunning.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Handles an incoming simulated gateway failure: finds or creates the
 * customer, records the failed payment and its failure event, then
 * schedules BOTH retry tracks — the naive fixed calendar and the AI-driven
 * one (Gemini classification -> exponential backoff schedule) — so the
 * dashboard can compare them head to head for the same failure event.
 */
@Service
public class FailureIngestService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final FailureEventRepository failureEventRepository;
    private final NaiveRetryPlanner naiveRetryPlanner;
    private final AiRetryPlanner aiRetryPlanner;
    private final FailureClassificationService failureClassificationService;

    public FailureIngestService(CustomerRepository customerRepository,
                                 PaymentRepository paymentRepository,
                                 FailureEventRepository failureEventRepository,
                                 NaiveRetryPlanner naiveRetryPlanner,
                                 AiRetryPlanner aiRetryPlanner,
                                 FailureClassificationService failureClassificationService) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.failureEventRepository = failureEventRepository;
        this.naiveRetryPlanner = naiveRetryPlanner;
        this.aiRetryPlanner = aiRetryPlanner;
        this.failureClassificationService = failureClassificationService;
    }

    @Transactional
    public IngestResult ingest(FailureIngestRequest request) {
        Customer customer = customerRepository.findByEmail(request.customerEmail())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .name(request.customerName())
                        .email(request.customerEmail())
                        .createdAt(Instant.now())
                        .build()));

        String currency = (request.currency() == null || request.currency().isBlank())
                ? "INR"
                : request.currency();

        Payment payment = paymentRepository.save(Payment.builder()
                .customer(customer)
                .amount(request.amount())
                .currency(currency)
                .status(PaymentStatus.FAILED)
                .createdAt(Instant.now())
                .build());

        Instant occurredAt = Instant.now();
        FailureEvent failureEvent = failureEventRepository.save(FailureEvent.builder()
                .payment(payment)
                .failureCode(request.failureCode())
                .occurredAt(occurredAt)
                .build());

        List<RetryAttempt> naiveAttempts = naiveRetryPlanner.scheduleNaiveRetries(payment, occurredAt);

        FailureClassification classification = failureClassificationService.classify(
                request.failureCode(), request.amount(), currency);
        List<RetryAttempt> aiAttempts = aiRetryPlanner.scheduleAiRetries(payment, occurredAt, classification);

        return new IngestResult(payment, failureEvent, naiveAttempts, aiAttempts, classification);
    }

    public record IngestResult(Payment payment, FailureEvent failureEvent, List<RetryAttempt> naiveAttempts,
                                List<RetryAttempt> aiAttempts, FailureClassification classification) {
    }
}
