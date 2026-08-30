package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.RetryAttempt;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        Long id,
        String customerName,
        String customerEmail,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        FailureCode failureCode,
        List<RetryAttemptResponse> retryAttempts
) {
    public static PaymentResponse from(Payment payment, FailureCode failureCode, List<RetryAttempt> attempts) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomer().getName(),
                payment.getCustomer().getEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                failureCode,
                attempts.stream().map(RetryAttemptResponse::from).toList()
        );
    }
}
