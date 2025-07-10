package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object representing a batch payment request.
 */
@Value
@Builder
@With
public class PaymentBatchRequest {
    
    String batchId;
    String batchName;
    String description;
    PaymentBatchType batchType;
    List<PaymentRequest> paymentRequests;
    BigDecimal totalAmount;
    String currencyCode;
    LocalDateTime scheduledExecutionDate;
    LocalDateTime requestedAt;
    String requestedBy;
    boolean requiresApproval;
    String approvedBy;
    LocalDateTime approvedAt;
    BatchRequestStatus status;
    Map<String, String> metadata;

    public enum BatchRequestStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, SCHEDULED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }

    public static class PaymentBatchRequestBuilder {
        public PaymentBatchRequestBuilder totalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total amount cannot be negative");
            }
            this.totalAmount = amount;
            return this;
        }

        public PaymentBatchRequest build() {
            if (batchId == null || batchId.trim().isEmpty()) {
                throw new IllegalArgumentException("Batch ID is required");
            }
            if (batchType == null) {
                throw new IllegalArgumentException("Batch type is required");
            }
            if (paymentRequests == null || paymentRequests.isEmpty()) {
                throw new IllegalArgumentException("Payment requests are required");
            }
            if (requestedAt == null) {
                this.requestedAt = LocalDateTime.now();
            }
            if (scheduledExecutionDate == null) {
                this.scheduledExecutionDate = LocalDateTime.now();
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (status == null) {
                this.status = BatchRequestStatus.DRAFT;
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Calculate total amount if not provided
            if (totalAmount == null) {
                this.totalAmount = paymentRequests.stream()
                        .map(PaymentRequest::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            
            // Determine if approval is required
            if (batchType.requiresApproval() || 
                totalAmount.compareTo(new BigDecimal("10000")) > 0 ||
                paymentRequests.size() > 100) {
                this.requiresApproval = true;
            }
            
            return new PaymentBatchRequest(
                batchId, batchName, description, batchType, paymentRequests,
                totalAmount, currencyCode, scheduledExecutionDate, requestedAt,
                requestedBy, requiresApproval, approvedBy, approvedAt, status, metadata
            );
        }
    }

    public boolean isDraft() {
        return status == BatchRequestStatus.DRAFT;
    }

    public boolean isPendingApproval() {
        return status == BatchRequestStatus.PENDING_APPROVAL;
    }

    public boolean isApproved() {
        return status == BatchRequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == BatchRequestStatus.REJECTED;
    }

    public boolean isScheduled() {
        return status == BatchRequestStatus.SCHEDULED;
    }

    public boolean isProcessing() {
        return status == BatchRequestStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == BatchRequestStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == BatchRequestStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == BatchRequestStatus.CANCELLED;
    }

    public boolean canBeSubmitted() {
        return status == BatchRequestStatus.DRAFT;
    }

    public boolean canBeApproved() {
        return status == BatchRequestStatus.PENDING_APPROVAL;
    }

    public boolean canBeRejected() {
        return status == BatchRequestStatus.PENDING_APPROVAL;
    }

    public boolean canBeCancelled() {
        return status == BatchRequestStatus.DRAFT || 
               status == BatchRequestStatus.PENDING_APPROVAL || 
               status == BatchRequestStatus.APPROVED || 
               status == BatchRequestStatus.SCHEDULED;
    }

    public boolean canBeExecuted() {
        return status == BatchRequestStatus.APPROVED || status == BatchRequestStatus.SCHEDULED;
    }

    public boolean isExecutionDue() {
        return scheduledExecutionDate != null && 
               !scheduledExecutionDate.isAfter(LocalDateTime.now()) &&
               canBeExecuted();
    }

    public boolean isScheduledInFuture() {
        return scheduledExecutionDate != null && 
               scheduledExecutionDate.isAfter(LocalDateTime.now());
    }

    public boolean isHighValue() {
        return totalAmount.compareTo(new BigDecimal("50000")) > 0;
    }

    public boolean isLargeVolume() {
        return paymentRequests.size() > 500;
    }

    public int getPaymentCount() {
        return paymentRequests.size();
    }

    public BigDecimal getAveragePaymentAmount() {
        if (paymentRequests.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(paymentRequests.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    public List<PaymentRequest> getRecurringPayments() {
        return paymentRequests.stream()
                .filter(PaymentRequest::isRecurring)
                .toList();
    }

    public List<PaymentRequest> getImmediatePayments() {
        return paymentRequests.stream()
                .filter(PaymentRequest::isImmediate)
                .toList();
    }

    public List<PaymentRequest> getScheduledPayments() {
        return paymentRequests.stream()
                .filter(PaymentRequest::isScheduled)
                .toList();
    }

    public List<PaymentRequest> getHighValuePayments() {
        return paymentRequests.stream()
                .filter(PaymentRequest::isHighValue)
                .toList();
    }
}