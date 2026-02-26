package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing a payment schedule modification request.
 */
@Value
@Builder
@With
public class PaymentScheduleModification {
    
    String modificationId;
    PaymentScheduleId scheduleId;
    ModificationType modificationType;
    String reason;
    String description;
    BigDecimal newPaymentAmount;
    PaymentFrequency newFrequency;
    LocalDateTime newStartDate;
    LocalDateTime newEndDate;
    PaymentMethod newPaymentMethod;
    PaymentSource newPaymentSource;
    PaymentAllocationStrategy newAllocationStrategy;
    boolean newIsAutomatic;
    boolean applyToFuturePayments;
    boolean applyToAllPayments;
    LocalDateTime effectiveDate;
    LocalDateTime requestedAt;
    String requestedBy;
    ModificationStatus status;
    Map<String, String> metadata;

    public enum ModificationType {
        AMOUNT_CHANGE("Amount Change"),
        FREQUENCY_CHANGE("Frequency Change"),
        DATE_CHANGE("Date Change"),
        PAYMENT_METHOD_CHANGE("Payment Method Change"),
        PAYMENT_SOURCE_CHANGE("Payment Source Change"),
        ALLOCATION_STRATEGY_CHANGE("Allocation Strategy Change"),
        AUTOMATIC_SETTING_CHANGE("Automatic Setting Change"),
        SCHEDULE_SUSPENSION("Schedule Suspension"),
        SCHEDULE_RESUMPTION("Schedule Resumption"),
        SCHEDULE_TERMINATION("Schedule Termination"),
        COMPLETE_RESTRUCTURE("Complete Restructure");

        private final String displayName;

        ModificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ModificationStatus {
        PENDING, APPROVED, REJECTED, APPLIED, CANCELLED
    }

    public static class PaymentScheduleModificationBuilder {
        public PaymentScheduleModificationBuilder newPaymentAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("New payment amount must be greater than zero");
            }
            this.newPaymentAmount = amount;
            return this;
        }

        public PaymentScheduleModification build() {
            if (modificationId == null || modificationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Modification ID is required");
            }
            if (scheduleId == null) {
                throw new IllegalArgumentException("Schedule ID is required");
            }
            if (modificationType == null) {
                throw new IllegalArgumentException("Modification type is required");
            }
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Reason is required");
            }
            if (effectiveDate == null) {
                this.effectiveDate = LocalDateTime.now();
            }
            if (requestedAt == null) {
                this.requestedAt = LocalDateTime.now();
            }
            if (status == null) {
                this.status = ModificationStatus.PENDING;
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            return new PaymentScheduleModification(
                modificationId, scheduleId, modificationType, reason, description,
                newPaymentAmount, newFrequency, newStartDate, newEndDate,
                newPaymentMethod, newPaymentSource, newAllocationStrategy,
                newIsAutomatic, applyToFuturePayments, applyToAllPayments,
                effectiveDate, requestedAt, requestedBy, status, metadata
            );
        }
    }

    public boolean isPending() {
        return status == ModificationStatus.PENDING;
    }

    public boolean isApproved() {
        return status == ModificationStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == ModificationStatus.REJECTED;
    }

    public boolean isApplied() {
        return status == ModificationStatus.APPLIED;
    }

    public boolean isCancelled() {
        return status == ModificationStatus.CANCELLED;
    }

    public boolean canBeApproved() {
        return status == ModificationStatus.PENDING;
    }

    public boolean canBeRejected() {
        return status == ModificationStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == ModificationStatus.PENDING || status == ModificationStatus.APPROVED;
    }

    public boolean isEffectiveNow() {
        return effectiveDate.isBefore(LocalDateTime.now()) || effectiveDate.isEqual(LocalDateTime.now());
    }

    public boolean isEffectiveInFuture() {
        return effectiveDate.isAfter(LocalDateTime.now());
    }

    public boolean isStructuralChange() {
        return modificationType == ModificationType.FREQUENCY_CHANGE ||
               modificationType == ModificationType.COMPLETE_RESTRUCTURE ||
               modificationType == ModificationType.SCHEDULE_TERMINATION;
    }

    public boolean requiresApproval() {
        return isStructuralChange() || 
               modificationType == ModificationType.AMOUNT_CHANGE ||
               modificationType == ModificationType.SCHEDULE_SUSPENSION;
    }
}