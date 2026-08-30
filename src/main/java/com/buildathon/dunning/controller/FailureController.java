package com.buildathon.dunning.controller;

import com.buildathon.dunning.dto.FailureIngestRequest;
import com.buildathon.dunning.dto.PaymentResponse;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.service.FailureIngestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simulates a payment gateway's failed-charge webhook.
 */
@RestController
@RequestMapping("/api/failures")
public class FailureController {

    private final FailureIngestService failureIngestService;

    public FailureController(FailureIngestService failureIngestService) {
        this.failureIngestService = failureIngestService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> ingest(@Valid @RequestBody FailureIngestRequest request) {
        FailureIngestService.IngestResult result = failureIngestService.ingest(request);
        FailureCode failureCode = result.failureEvent().getFailureCode();

        List<RetryAttempt> allAttempts = new ArrayList<>();
        allAttempts.addAll(result.naiveAttempts());
        allAttempts.addAll(result.aiAttempts());
        allAttempts.sort(Comparator.comparing(RetryAttempt::getStrategy).thenComparing(RetryAttempt::getAttemptNumber));

        PaymentResponse response = PaymentResponse.from(result.payment(), failureCode, allAttempts);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
