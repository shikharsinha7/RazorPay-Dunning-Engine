package com.buildathon.dunning.entity;

import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.RetryOutcome;
import com.buildathon.dunning.entity.enums.Strategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single retry attempt for a failed payment. Every failed payment produces
 * TWO parallel attempt tracks (strategy = AI vs strategy = NAIVE) so the
 * dashboard can compare recovery rates head to head.
 */
@Entity
@Table(name = "retry_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Strategy strategy;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    // Populated only for AI-strategy attempts, filled in on Day 3
    @Column(name = "ai_retry_window_hours")
    private Integer aiRetryWindowHours;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    @Column(name = "ai_suggest_alt_payment_method")
    private Boolean aiSuggestAltPaymentMethod;

    // columnDefinition set explicitly so Hibernate does NOT auto-generate an
    // implicit CHECK constraint frozen to whatever enum values exist at
    // table-creation time (ddl-auto=update never refreshes that constraint
    // when the enum gains new values later — see DAY2.md for the incident).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Builder.Default
    private RetryOutcome outcome = RetryOutcome.PENDING;
}
