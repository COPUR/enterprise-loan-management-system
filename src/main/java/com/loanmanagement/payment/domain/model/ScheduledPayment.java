package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing an individual scheduled payment.
 */
@Value
@Builder
@With
public class ScheduledPayment {
    
    String paymentId;
    String scheduleId;
    String loanId;
    BigDecimal amount;
    String currencyCode;
    LocalDateTime scheduledDate;
    LocalDateTime actualDate;
    ScheduledPaymentStatus status;
    PaymentMethod paymentMethod;
    PaymentSource paymentSource;
    String description;
    int sequenceNumber;
    boolean isRecurring;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Map<String, String> metadata;

    public enum ScheduledPaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, SKIPPED
    }

    public static class ScheduledPaymentBuilder {
        public ScheduledPaymentBuilder amount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Scheduled payment amount must be greater than zero");
            }
            this.amount = amount;
            return this;
        }

        public ScheduledPayment build() {
            if (paymentId == null || paymentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Payment ID is required");
            }
            if (scheduleId == null || scheduleId.trim().isEmpty()) {
                throw new IllegalArgumentException("Schedule ID is required");
            }
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (amount == null) {
                throw new IllegalArgumentException("Payment amount is required");
            }
            if (scheduledDate == null) {
                throw new IllegalArgumentException("Scheduled date is required");
            }
            if (status == null) {
                this.status = ScheduledPaymentStatus.PENDING;
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (createdAt == null) {
                this.createdAt = LocalDateTime.now();
            }
            if (updatedAt == null) {
                this.updatedAt = LocalDateTime.now();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            return new ScheduledPayment(
                paymentId, scheduleId, loanId, amount, currencyCode,
                scheduledDate, actualDate, status, paymentMethod, paymentSource,
                description, sequenceNumber, isRecurring, createdAt, updatedAt, metadata
            );
        }
    }

    public boolean isOverdue() {
        return status == ScheduledPaymentStatus.PENDING && 
               scheduledDate.isBefore(LocalDateTime.now());
    }

    public boolean isCompleted() {
        return status == ScheduledPaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ScheduledPaymentStatus.FAILED;
    }

    public boolean isPending() {
        return status == ScheduledPaymentStatus.PENDING;
    }

    public boolean canBeProcessed() {
        return status == ScheduledPaymentStatus.PENDING && 
               !scheduledDate.isAfter(LocalDateTime.now().plusMinutes(5));
    }

    public boolean canBeCancelled() {
        return status == ScheduledPaymentStatus.PENDING || status == ScheduledPaymentStatus.PROCESSING;
    }

    public int getDaysUntilDue() {
        if (scheduledDate.isBefore(LocalDateTime.now())) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now().toLocalDate(), 
            scheduledDate.toLocalDate()
        );
    }
}