package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.enums.FailureCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Simulates the payload a real payment gateway webhook would send on a
 * failed recurring charge.
 */
public record FailureIngestRequest(
        @NotBlank String customerName,
        @NotBlank @Email String customerEmail,
        @NotNull @Positive BigDecimal amount,
        String currency,
        @NotNull FailureCode failureCode
) {
}
