package com.buildathon.dunning.controller;

import com.buildathon.dunning.dto.PaymentResponse;
import com.buildathon.dunning.dto.RecoveryMessageResponse;
import com.buildathon.dunning.entity.FailureEvent;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.repository.FailureEventRepository;
import com.buildathon.dunning.repository.PaymentRepository;
import com.buildathon.dunning.repository.RecoveryMessageRepository;
import com.buildathon.dunning.repository.RetryAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read endpoints for verifying ingest and retry state — used for testing
 * now, and will back parts of the dashboard from Day 5 onward.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final FailureEventRepository failureEventRepository;
    private final RetryAttemptRepository retryAttemptRepository;
    private final RecoveryMessageRepository recoveryMessageRepository;

    public PaymentController(PaymentRepository paymentRepository,
                              FailureEventRepository failureEventRepository,
                              RetryAttemptRepository retryAttemptRepository,
                              RecoveryMessageRepository recoveryMessageRepository) {
        this.paymentRepository = paymentRepository;
        this.failureEventRepository = failureEventRepository;
        this.retryAttemptRepository = retryAttemptRepository;
        this.recoveryMessageRepository = recoveryMessageRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        FailureCode failureCode = failureEventRepository.findFirstByPayment_Id(id)
                .map(FailureEvent::getFailureCode)
                .orElse(null);
        List<RetryAttempt> attempts = retryAttemptRepository.findByPayment_IdOrderByAttemptNumberAsc(id);
        return ResponseEntity.ok(PaymentResponse.from(payment, failureCode, attempts));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listPayments() {
        List<PaymentResponse> responses = paymentRepository.findAll().stream()
                .map(payment -> {
                    FailureCode failureCode = failureEventRepository.findFirstByPayment_Id(payment.getId())
                            .map(FailureEvent::getFailureCode)
                            .orElse(null);
                    List<RetryAttempt> attempts =
                            retryAttemptRepository.findByPayment_IdOrderByAttemptNumberAsc(payment.getId());
                    return PaymentResponse.from(payment, failureCode, attempts);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<RecoveryMessageResponse>> getMessages(@PathVariable Long id) {
        List<RecoveryMessageResponse> messages = recoveryMessageRepository
                .findByRetryAttempt_Payment_IdOrderBySentAtAsc(id)
                .stream()
                .map(RecoveryMessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }
}
