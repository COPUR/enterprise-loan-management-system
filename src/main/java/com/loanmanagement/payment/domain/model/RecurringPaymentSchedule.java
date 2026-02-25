package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object representing recurring payment schedule details.
 */
@Value
@Builder
@With
public class RecurringPaymentSchedule {
    
    String recurringScheduleId;
    String loanId;
    String customerId;
    PaymentFrequency frequency;
    BigDecimal recurringAmount;
    String currencyCode;
    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime nextExecutionDate;
    PaymentMethod paymentMethod;
    PaymentSource paymentSource;
    PaymentAllocationStrategy allocationStrategy;
    boolean isActive;
    boolean isAutomatic;
    int maxRetries;
    int currentRetryCount;
    LocalDateTime lastExecutionDate;
    LocalDateTime lastSuccessfulExecutionDate;
    RecurringStatus status;
    List<String> executionHistory;
    Map<String, String> metadata;

    public enum RecurringStatus {
        ACTIVE, PAUSED, CANCELLED, COMPLETED, FAILED, SUSPENDED
    }

    public static class RecurringPaymentScheduleBuilder {
        public RecurringPaymentScheduleBuilder recurringAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Recurring amount must be greater than zero");
            }
            this.recurringAmount = amount;
            return this;
        }

        public RecurringPaymentScheduleBuilder maxRetries(int retries) {
            if (retries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            this.maxRetries = retries;
            return this;
        }

        public RecurringPaymentSchedule build() {
            if (recurringScheduleId == null || recurringScheduleId.trim().isEmpty()) {
                throw new IllegalArgumentException("Recurring schedule ID is required");
            }
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (frequency == null) {
                throw new IllegalArgumentException("Payment frequency is required");
            }
            if (frequency == PaymentFrequency.ONE_TIME) {
                throw new IllegalArgumentException("One-time frequency cannot be used for recurring schedules");
            }
            if (recurringAmount == null) {
                throw new IllegalArgumentException("Recurring amount is required");
            }
            if (startDate == null) {
                throw new IllegalArgumentException("Start date is required");
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (allocationStrategy == null) {
                this.allocationStrategy = PaymentAllocationStrategy.INTEREST_FIRST;
            }
            if (status == null) {
                this.status = RecurringStatus.ACTIVE;
            }
            if (maxRetries == 0) {
                this.maxRetries = 3;
            }
            if (executionHistory == null) {
                this.executionHistory = List.of();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            return new RecurringPaymentSchedule(
                recurringScheduleId, loanId, customerId, frequency, recurringAmount,
                currencyCode, startDate, endDate, nextExecutionDate, paymentMethod,
                paymentSource, allocationStrategy, isActive, isAutomatic, maxRetries,
                currentRetryCount, lastExecutionDate, lastSuccessfulExecutionDate,
                status, executionHistory, metadata
            );
        }
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isDue() {
        return nextExecutionDate != null && 
               !nextExecutionDate.isAfter(LocalDateTime.now()) &&
               status == RecurringStatus.ACTIVE;
    }

    public boolean hasReachedMaxRetries() {
        return currentRetryCount >= maxRetries;
    }

    public boolean canExecute() {
        return isActive && 
               status == RecurringStatus.ACTIVE && 
               !isExpired() && 
               !hasReachedMaxRetries();
    }

    public boolean canBePaused() {
        return status == RecurringStatus.ACTIVE;
    }

    public boolean canBeResumed() {
        return status == RecurringStatus.PAUSED;
    }

    public boolean canBeCancelled() {
        return status == RecurringStatus.ACTIVE || status == RecurringStatus.PAUSED;
    }

    public LocalDateTime getNextExecutionDate() {
        if (nextExecutionDate != null) {
            return nextExecutionDate;
        }
        if (lastExecutionDate != null) {
            return lastExecutionDate.plus(frequency.getPeriod());
        }
        return startDate;
    }

    public int getExecutionCount() {
        return executionHistory.size();
    }

    public int getSuccessfulExecutionCount() {
        return (int) executionHistory.stream()
                .filter(execution -> execution.contains("SUCCESS"))
                .count();
    }

    public int getFailedExecutionCount() {
        return (int) executionHistory.stream()
                .filter(execution -> execution.contains("FAILED"))
                .count();
    }

    public double getSuccessRate() {
        if (getExecutionCount() == 0) return 0.0;
        return (double) getSuccessfulExecutionCount() / getExecutionCount() * 100.0;
    }

    public boolean hasRecentFailures() {
        return getFailedExecutionCount() > 0 && 
               lastExecutionDate != null &&
               lastExecutionDate.isAfter(LocalDateTime.now().minusDays(7));
    }
}