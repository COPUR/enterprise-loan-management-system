package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object representing payment processing result.
 */
@Value
@Builder
@With
public class PaymentResult {
    
    PaymentId paymentId;
    String transactionId;
    String requestId;
    PaymentStatus status;
    BigDecimal processedAmount;
    BigDecimal requestedAmount;
    String currencyCode;
    LocalDateTime processedAt;
    LocalDateTime completedAt;
    PaymentAllocation allocation;
    List<PaymentFailureReason> failureReasons;
    String processorResponse;
    String processorTransactionId;
    Map<String, String> metadata;

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED
    }

    public static class PaymentResultBuilder {
        public PaymentResultBuilder processedAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Processed amount cannot be negative");
            }
            this.processedAmount = amount;
            return this;
        }

        public PaymentResultBuilder requestedAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Requested amount cannot be negative");
            }
            this.requestedAmount = amount;
            return this;
        }

        public PaymentResult build() {
            if (paymentId == null) {
                throw new IllegalArgumentException("Payment ID is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Payment status is required");
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
            if (status == PaymentStatus.FAILED && failureReasons.isEmpty()) {
                throw new IllegalArgumentException("Failed payments must have failure reasons");
            }
            if (status == PaymentStatus.COMPLETED && completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
            
            return new PaymentResult(
                paymentId, transactionId, requestId, status,
                processedAmount, requestedAmount, currencyCode,
                processedAt, completedAt, allocation, failureReasons,
                processorResponse, processorTransactionId, metadata
            );
        }
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    public boolean isPartiallyProcessed() {
        return processedAmount != null && requestedAmount != null &&
               processedAmount.compareTo(requestedAmount) < 0 &&
               processedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasFailureReasons() {
        return failureReasons != null && !failureReasons.isEmpty();
    }

    public static PaymentResult success(PaymentId paymentId, BigDecimal amount, PaymentAllocation allocation) {
        return PaymentResult.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.COMPLETED)
                .processedAmount(amount)
                .requestedAmount(amount)
                .allocation(allocation)
                .build();
    }

    public static PaymentResult failure(PaymentId paymentId, List<PaymentFailureReason> reasons) {
        return PaymentResult.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.FAILED)
                .failureReasons(reasons)
                .build();
    }

    public static PaymentResult pending(PaymentId paymentId) {
        return PaymentResult.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.PENDING)
                .build();
    }
}