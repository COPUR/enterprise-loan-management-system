package com.bank.compliance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ComplianceResult {
    private final ComplianceResultId id;
    private final String transactionId;
    private final String customerId;
    private final ComplianceDecision decision;
    private final List<String> reasons;
    private final Instant checkedAt;

    private ComplianceResult(
            ComplianceResultId id,
            String transactionId,
            String customerId,
            ComplianceDecision decision,
            List<String> reasons,
            Instant checkedAt
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.decision = Objects.requireNonNull(decision, "decision is required");
        this.reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons are required"));
        this.checkedAt = Objects.requireNonNull(checkedAt, "checkedAt is required");
    }

    public static ComplianceResult create(
            String transactionId,
            String customerId,
            ComplianceDecision decision,
            List<String> reasons
    ) {
        return new ComplianceResult(
                ComplianceResultId.generate(),
                transactionId,
                customerId,
                decision,
                reasons,
                Instant.now()
        );
    }

    public ComplianceResultId getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ComplianceDecision getDecision() {
        return decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public boolean isPassed() {
        return decision == ComplianceDecision.PASS;
    }
}
