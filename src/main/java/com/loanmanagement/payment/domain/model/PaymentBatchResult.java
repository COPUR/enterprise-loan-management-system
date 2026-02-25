package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object representing batch payment processing result.
 */
@Value
@Builder
@With
public class PaymentBatchResult {
    
    String batchId;
    String batchExecutionId;
    BatchExecutionStatus status;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    int totalPayments;
    int successfulPayments;
    int failedPayments;
    int pendingPayments;
    BigDecimal totalAmount;
    BigDecimal processedAmount;
    BigDecimal failedAmount;
    BigDecimal pendingAmount;
    String currencyCode;
    List<PaymentResult> paymentResults;
    List<String> batchErrors;
    Map<String, Integer> errorSummary;
    String executionReport;
    Map<String, String> metadata;

    public enum BatchExecutionStatus {
        PROCESSING, COMPLETED, PARTIALLY_COMPLETED, FAILED, CANCELLED
    }

    public static class PaymentBatchResultBuilder {
        public PaymentBatchResultBuilder totalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total amount cannot be negative");
            }
            this.totalAmount = amount;
            return this;
        }

        public PaymentBatchResultBuilder processedAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Processed amount cannot be negative");
            }
            this.processedAmount = amount;
            return this;
        }

        public PaymentBatchResult build() {
            if (batchId == null || batchId.trim().isEmpty()) {
                throw new IllegalArgumentException("Batch ID is required");
            }
            if (batchExecutionId == null || batchExecutionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Batch execution ID is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Batch execution status is required");
            }
            if (startedAt == null) {
                this.startedAt = LocalDateTime.now();
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (paymentResults == null) {
                this.paymentResults = List.of();
            }
            if (batchErrors == null) {
                this.batchErrors = List.of();
            }
            if (errorSummary == null) {
                this.errorSummary = Map.of();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Calculate counts from payment results if not provided
            if (totalPayments == 0 && !paymentResults.isEmpty()) {
                this.totalPayments = paymentResults.size();
            }
            if (successfulPayments == 0) {
                this.successfulPayments = (int) paymentResults.stream()
                        .filter(PaymentResult::isSuccessful)
                        .count();
            }
            if (failedPayments == 0) {
                this.failedPayments = (int) paymentResults.stream()
                        .filter(PaymentResult::isFailed)
                        .count();
            }
            if (pendingPayments == 0) {
                this.pendingPayments = (int) paymentResults.stream()
                        .filter(PaymentResult::isPending)
                        .count();
            }
            
            // Calculate amounts from payment results if not provided
            if (processedAmount == null) {
                this.processedAmount = paymentResults.stream()
                        .filter(PaymentResult::isSuccessful)
                        .map(PaymentResult::getProcessedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            if (failedAmount == null) {
                this.failedAmount = paymentResults.stream()
                        .filter(PaymentResult::isFailed)
                        .map(result -> result.getRequestedAmount() != null ? result.getRequestedAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            if (pendingAmount == null) {
                this.pendingAmount = paymentResults.stream()
                        .filter(PaymentResult::isPending)
                        .map(result -> result.getRequestedAmount() != null ? result.getRequestedAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            
            return new PaymentBatchResult(
                batchId, batchExecutionId, status, startedAt, completedAt,
                totalPayments, successfulPayments, failedPayments, pendingPayments,
                totalAmount, processedAmount, failedAmount, pendingAmount,
                currencyCode, paymentResults, batchErrors, errorSummary,
                executionReport, metadata
            );
        }
    }

    public boolean isCompleted() {
        return status == BatchExecutionStatus.COMPLETED;
    }

    public boolean isPartiallyCompleted() {
        return status == BatchExecutionStatus.PARTIALLY_COMPLETED;
    }

    public boolean isFailed() {
        return status == BatchExecutionStatus.FAILED;
    }

    public boolean isProcessing() {
        return status == BatchExecutionStatus.PROCESSING;
    }

    public boolean isCancelled() {
        return status == BatchExecutionStatus.CANCELLED;
    }

    public boolean isSuccessful() {
        return isCompleted() && failedPayments == 0;
    }

    public boolean hasFailures() {
        return failedPayments > 0;
    }

    public boolean hasPendingPayments() {
        return pendingPayments > 0;
    }

    public double getSuccessRate() {
        if (totalPayments == 0) return 0.0;
        return (double) successfulPayments / totalPayments * 100.0;
    }

    public double getFailureRate() {
        if (totalPayments == 0) return 0.0;
        return (double) failedPayments / totalPayments * 100.0;
    }

    public double getProcessingRate() {
        if (totalPayments == 0) return 0.0;
        return (double) (totalPayments - pendingPayments) / totalPayments * 100.0;
    }

    public BigDecimal getProcessedPercentage() {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return processedAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public List<PaymentResult> getSuccessfulResults() {
        return paymentResults.stream()
                .filter(PaymentResult::isSuccessful)
                .toList();
    }

    public List<PaymentResult> getFailedResults() {
        return paymentResults.stream()
                .filter(PaymentResult::isFailed)
                .toList();
    }

    public List<PaymentResult> getPendingResults() {
        return paymentResults.stream()
                .filter(PaymentResult::isPending)
                .toList();
    }

    public boolean hasBatchErrors() {
        return batchErrors != null && !batchErrors.isEmpty();
    }

    public long getExecutionDurationMinutes() {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).toMinutes();
    }

    public boolean isLongRunning() {
        return getExecutionDurationMinutes() > 60;
    }

    public static PaymentBatchResult success(String batchId, String executionId, List<PaymentResult> results) {
        return PaymentBatchResult.builder()
                .batchId(batchId)
                .batchExecutionId(executionId)
                .status(BatchExecutionStatus.COMPLETED)
                .paymentResults(results)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static PaymentBatchResult failure(String batchId, String executionId, List<String> errors) {
        return PaymentBatchResult.builder()
                .batchId(batchId)
                .batchExecutionId(executionId)
                .status(BatchExecutionStatus.FAILED)
                .batchErrors(errors)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static PaymentBatchResult partiallyCompleted(String batchId, String executionId, 
                                                       List<PaymentResult> results, List<String> errors) {
        return PaymentBatchResult.builder()
                .batchId(batchId)
                .batchExecutionId(executionId)
                .status(BatchExecutionStatus.PARTIALLY_COMPLETED)
                .paymentResults(results)
                .batchErrors(errors)
                .completedAt(LocalDateTime.now())
                .build();
    }
}