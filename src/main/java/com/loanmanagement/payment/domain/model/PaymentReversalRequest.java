package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing a payment reversal request.
 */
@Value
@Builder
@With
public class PaymentReversalRequest {
    
    String reversalRequestId;
    PaymentId originalPaymentId;
    String loanId;
    String customerId;
    PaymentReversalReason reason;
    String description;
    BigDecimal reversalAmount;
    BigDecimal originalAmount;
    String currencyCode;
    boolean isPartialReversal;
    LocalDateTime requestedAt;
    LocalDateTime effectiveDate;
    String requestedBy;
    String approvedBy;
    ReversalRequestStatus status;
    Map<String, String> metadata;

    public enum ReversalRequestStatus {
        PENDING, APPROVED, REJECTED, PROCESSED, CANCELLED, FAILED
    }

    public static class PaymentReversalRequestBuilder {
        public PaymentReversalRequestBuilder reversalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Reversal amount must be greater than zero");
            }
            this.reversalAmount = amount;
            return this;
        }

        public PaymentReversalRequestBuilder originalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Original amount must be greater than zero");
            }
            this.originalAmount = amount;
            return this;
        }

        public PaymentReversalRequest build() {
            if (reversalRequestId == null || reversalRequestId.trim().isEmpty()) {
                throw new IllegalArgumentException("Reversal request ID is required");
            }
            if (originalPaymentId == null) {
                throw new IllegalArgumentException("Original payment ID is required");
            }
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (reason == null) {
                throw new IllegalArgumentException("Reversal reason is required");
            }
            if (reversalAmount == null) {
                throw new IllegalArgumentException("Reversal amount is required");
            }
            if (originalAmount == null) {
                throw new IllegalArgumentException("Original amount is required");
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (requestedAt == null) {
                this.requestedAt = LocalDateTime.now();
            }
            if (effectiveDate == null) {
                this.effectiveDate = LocalDateTime.now();
            }
            if (status == null) {
                this.status = ReversalRequestStatus.PENDING;
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Determine if partial reversal
            if (reversalAmount.compareTo(originalAmount) < 0) {
                this.isPartialReversal = true;
            }
            
            // Validate reversal amount
            if (reversalAmount.compareTo(originalAmount) > 0) {
                throw new IllegalArgumentException("Reversal amount cannot exceed original amount");
            }
            
            return new PaymentReversalRequest(
                reversalRequestId, originalPaymentId, loanId, customerId,
                reason, description, reversalAmount, originalAmount, currencyCode,
                isPartialReversal, requestedAt, effectiveDate, requestedBy,
                approvedBy, status, metadata
            );
        }
    }

    public boolean isPending() {
        return status == ReversalRequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == ReversalRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == ReversalRequestStatus.REJECTED;
    }

    public boolean isProcessed() {
        return status == ReversalRequestStatus.PROCESSED;
    }

    public boolean isCancelled() {
        return status == ReversalRequestStatus.CANCELLED;
    }

    public boolean isFailed() {
        return status == ReversalRequestStatus.FAILED;
    }

    public boolean canBeApproved() {
        return status == ReversalRequestStatus.PENDING;
    }

    public boolean canBeRejected() {
        return status == ReversalRequestStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == ReversalRequestStatus.PENDING || status == ReversalRequestStatus.APPROVED;
    }

    public boolean canBeProcessed() {
        return status == ReversalRequestStatus.APPROVED;
    }

    public boolean requiresApproval() {
        return reason.isSystemInitiated() || isPartialReversal || 
               reversalAmount.compareTo(new BigDecimal("1000")) > 0;
    }

    public boolean isSystemInitiated() {
        return reason.isSystemInitiated();
    }

    public boolean isCustomerInitiated() {
        return reason.isCustomerInitiated();
    }

    public boolean isEffectiveNow() {
        return effectiveDate.isBefore(LocalDateTime.now()) || effectiveDate.isEqual(LocalDateTime.now());
    }

    public boolean isEffectiveInFuture() {
        return effectiveDate.isAfter(LocalDateTime.now());
    }

    public BigDecimal getReversalPercentage() {
        if (originalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return reversalAmount.divide(originalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public boolean isFullReversal() {
        return !isPartialReversal && reversalAmount.compareTo(originalAmount) == 0;
    }
}