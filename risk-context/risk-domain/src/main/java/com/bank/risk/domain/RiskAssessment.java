package com.bank.risk.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class RiskAssessment {
    private final RiskAssessmentId id;
    private final String transactionId;
    private final BigDecimal amount;
    private final String currency;
    private final int score;
    private final RiskDecision decision;
    private final List<String> reasons;
    private final Instant assessedAt;

    private RiskAssessment(
            RiskAssessmentId id,
            String transactionId,
            BigDecimal amount,
            String currency,
            int score,
            RiskDecision decision,
            List<String> reasons,
            Instant assessedAt
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.score = score;
        this.decision = Objects.requireNonNull(decision, "decision is required");
        this.reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons are required"));
        this.assessedAt = Objects.requireNonNull(assessedAt, "assessedAt is required");
    }

    public static RiskAssessment create(
            String transactionId,
            BigDecimal amount,
            String currency,
            int score,
            RiskDecision decision,
            List<String> reasons
    ) {
        return new RiskAssessment(
                RiskAssessmentId.generate(),
                transactionId,
                amount,
                currency,
                score,
                decision,
                reasons,
                Instant.now()
        );
    }

    public RiskAssessmentId getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public int getScore() {
        return score;
    }

    public RiskDecision getDecision() {
        return decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public boolean isBlocked() {
        return decision == RiskDecision.BLOCK;
    }

    public boolean requiresManualReview() {
        return decision == RiskDecision.REVIEW;
    }
}
