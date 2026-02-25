package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Value object representing a payment schedule aggregate.
 */
@Value
@Builder
@With
public class PaymentSchedule {
    
    PaymentScheduleId scheduleId;
    String loanId;
    String customerId;
    String scheduleName;
    String description;
    PaymentScheduleStatus status;
    PaymentFrequency frequency;
    BigDecimal paymentAmount;
    String currencyCode;
    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime nextPaymentDate;
    PaymentMethod paymentMethod;
    PaymentSource paymentSource;
    PaymentAllocationStrategy allocationStrategy;
    List<ScheduledPayment> scheduledPayments;
    boolean isAutomatic;
    boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Map<String, String> metadata;

    public static class PaymentScheduleBuilder {
        public PaymentScheduleBuilder paymentAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero");
            }
            this.paymentAmount = amount;
            return this;
        }

        public PaymentSchedule build() {
            if (scheduleId == null) {
                throw new IllegalArgumentException("Schedule ID is required");
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
            if (paymentAmount == null) {
                throw new IllegalArgumentException("Payment amount is required");
            }
            if (startDate == null) {
                throw new IllegalArgumentException("Start date is required");
            }
            if (status == null) {
                this.status = PaymentScheduleStatus.DRAFT;
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (allocationStrategy == null) {
                this.allocationStrategy = PaymentAllocationStrategy.INTEREST_FIRST;
            }
            if (scheduledPayments == null) {
                this.scheduledPayments = List.of();
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
            
            return new PaymentSchedule(
                scheduleId, loanId, customerId, scheduleName, description,
                status, frequency, paymentAmount, currencyCode, startDate, endDate,
                nextPaymentDate, paymentMethod, paymentSource, allocationStrategy,
                scheduledPayments, isAutomatic, isActive, createdAt, updatedAt, metadata
            );
        }
    }

    public boolean isRecurring() {
        return frequency.isRecurring();
    }

    public boolean hasEndDate() {
        return endDate != null;
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean canBeActivated() {
        return status == PaymentScheduleStatus.DRAFT || status == PaymentScheduleStatus.INACTIVE;
    }

    public boolean canBeDeactivated() {
        return status == PaymentScheduleStatus.ACTIVE;
    }

    public boolean canBeModified() {
        return status.canBeModified();
    }

    public List<ScheduledPayment> getPendingPayments() {
        return scheduledPayments.stream()
                .filter(ScheduledPayment::isPending)
                .collect(Collectors.toList());
    }

    public List<ScheduledPayment> getOverduePayments() {
        return scheduledPayments.stream()
                .filter(ScheduledPayment::isOverdue)
                .collect(Collectors.toList());
    }

    public List<ScheduledPayment> getCompletedPayments() {
        return scheduledPayments.stream()
                .filter(ScheduledPayment::isCompleted)
                .collect(Collectors.toList());
    }

    public List<ScheduledPayment> getFailedPayments() {
        return scheduledPayments.stream()
                .filter(ScheduledPayment::isFailed)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalScheduledAmount() {
        return scheduledPayments.stream()
                .map(ScheduledPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCompletedAmount() {
        return getCompletedPayments().stream()
                .map(ScheduledPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPendingAmount() {
        return getPendingPayments().stream()
                .map(ScheduledPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalPayments() {
        return scheduledPayments.size();
    }

    public int getCompletedPaymentCount() {
        return getCompletedPayments().size();
    }

    public int getPendingPaymentCount() {
        return getPendingPayments().size();
    }

    public double getCompletionPercentage() {
        if (getTotalPayments() == 0) return 0.0;
        return (double) getCompletedPaymentCount() / getTotalPayments() * 100.0;
    }
}