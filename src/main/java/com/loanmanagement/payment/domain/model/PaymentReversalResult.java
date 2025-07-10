package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object representing payment reversal processing result.
 */
@Value
@Builder
@With
public class PaymentReversalResult {
    
    String reversalId;
    PaymentId originalPaymentId;
    String reversalRequestId;
    ReversalStatus status;
    BigDecimal reversedAmount;
    BigDecimal requestedAmount;
    String currencyCode;
    LocalDateTime processedAt;
    LocalDateTime completedAt;
    PaymentAllocation reversalAllocation;
    List<PaymentFailureReason> failureReasons;
    String processorResponse;
    String processorTransactionId;
    Map<String, String> metadata;

    public enum ReversalStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, PARTIALLY_COMPLETED
    }

    public static class PaymentReversalResultBuilder {
        public PaymentReversalResultBuilder reversedAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Reversed amount cannot be negative");
            }
            this.reversedAmount = amount;
            return this;
        }

        public PaymentReversalResultBuilder requestedAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Requested amount cannot be negative");
            }
            this.requestedAmount = amount;
            return this;
        }

        public PaymentReversalResult build() {
            if (reversalId == null || reversalId.trim().isEmpty()) {
                throw new IllegalArgumentException("Reversal ID is required");
            }
            if (originalPaymentId == null) {
                throw new IllegalArgumentException("Original payment ID is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Reversal status is required");
            }
            if (processedAt == null) {
                this.processedAt = LocalDateTime.now();
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (failureReasons == null) {
                this.failureReasons = List.of();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Validate status consistency
            if (status == ReversalStatus.FAILED && failureReasons.isEmpty()) {
                throw new IllegalArgumentException("Failed reversals must have failure reasons");
            }
            if (status == ReversalStatus.COMPLETED && completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
            
            return new PaymentReversalResult(
                reversalId, originalPaymentId, reversalRequestId, status,
                reversedAmount, requestedAmount, currencyCode, processedAt,
                completedAt, reversalAllocation, failureReasons, processorResponse,
                processorTransactionId, metadata
            );
        }
    }

    public boolean isSuccessful() {
        return status == ReversalStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ReversalStatus.FAILED;
    }

    public boolean isPending() {
        return status == ReversalStatus.PENDING || status == ReversalStatus.PROCESSING;
    }

    public boolean isPartiallyCompleted() {
        return status == ReversalStatus.PARTIALLY_COMPLETED;
    }

    public boolean isPartiallyReversed() {
        return reversedAmount != null && requestedAmount != null &&
               reversedAmount.compareTo(requestedAmount) < 0 &&
               reversedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasFailureReasons() {
        return failureReasons != null && !failureReasons.isEmpty();
    }

    public BigDecimal getUnreversedAmount() {
        if (reversedAmount == null || requestedAmount == null) {
            return BigDecimal.ZERO;
        }
        return requestedAmount.subtract(reversedAmount);
    }

    public BigDecimal getReversalPercentage() {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (reversedAmount == null) {
            return BigDecimal.ZERO;
        }
        return reversedAmount.divide(requestedAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public boolean isFullyReversed() {
        return reversedAmount != null && requestedAmount != null &&
               reversedAmount.compareTo(requestedAmount) == 0;
    }

    public static PaymentReversalResult success(String reversalId, PaymentId originalPaymentId, 
                                               BigDecimal amount, PaymentAllocation allocation) {
        return PaymentReversalResult.builder()
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .status(ReversalStatus.COMPLETED)
                .reversedAmount(amount)
                .requestedAmount(amount)
                .reversalAllocation(allocation)
                .build();
    }

    public static PaymentReversalResult failure(String reversalId, PaymentId originalPaymentId, 
                                               List<PaymentFailureReason> reasons) {
        return PaymentReversalResult.builder()
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .status(ReversalStatus.FAILED)
                .failureReasons(reasons)
                .build();
    }

    public static PaymentReversalResult pending(String reversalId, PaymentId originalPaymentId) {
        return PaymentReversalResult.builder()
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .status(ReversalStatus.PENDING)
                .build();
    }

    public static PaymentReversalResult partiallyCompleted(String reversalId, PaymentId originalPaymentId, 
                                                          BigDecimal reversedAmount, BigDecimal requestedAmount) {
        return PaymentReversalResult.builder()
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .status(ReversalStatus.PARTIALLY_COMPLETED)
                .reversedAmount(reversedAmount)
                .requestedAmount(requestedAmount)
                .build();
    }
}