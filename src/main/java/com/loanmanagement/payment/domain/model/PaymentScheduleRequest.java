package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing a payment schedule creation request.
 */
@Value
@Builder
@With
public class PaymentScheduleRequest {
    
    String requestId;
    String loanId;
    String customerId;
    String scheduleName;
    String description;
    PaymentFrequency frequency;
    BigDecimal paymentAmount;
    String currencyCode;
    LocalDateTime startDate;
    LocalDateTime endDate;
    PaymentMethod paymentMethod;
    PaymentSource paymentSource;
    PaymentAllocationStrategy allocationStrategy;
    boolean isAutomatic;
    boolean generateImmediately;
    int numberOfPayments;
    LocalDateTime requestedAt;
    Map<String, String> metadata;

    public static class PaymentScheduleRequestBuilder {
        public PaymentScheduleRequestBuilder paymentAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero");
            }
            this.paymentAmount = amount;
            return this;
        }

        public PaymentScheduleRequestBuilder numberOfPayments(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Number of payments must be greater than zero");
            }
            this.numberOfPayments = count;
            return this;
        }

        public PaymentScheduleRequest build() {
            if (requestId == null || requestId.trim().isEmpty()) {
                throw new IllegalArgumentException("Request ID is required");
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
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (allocationStrategy == null) {
                this.allocationStrategy = PaymentAllocationStrategy.INTEREST_FIRST;
            }
            if (requestedAt == null) {
                this.requestedAt = LocalDateTime.now();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Validate frequency and end date/number of payments
            if (frequency == PaymentFrequency.ONE_TIME && numberOfPayments > 1) {
                throw new IllegalArgumentException("One-time frequency cannot have multiple payments");
            }
            if (frequency != PaymentFrequency.ONE_TIME && endDate == null && numberOfPayments <= 0) {
                throw new IllegalArgumentException("Recurring schedules must have either end date or number of payments");
            }
            
            return new PaymentScheduleRequest(
                requestId, loanId, customerId, scheduleName, description,
                frequency, paymentAmount, currencyCode, startDate, endDate,
                paymentMethod, paymentSource, allocationStrategy, isAutomatic,
                generateImmediately, numberOfPayments, requestedAt, metadata
            );
        }
    }

    public boolean isRecurring() {
        return frequency.isRecurring();
    }

    public boolean hasEndDate() {
        return endDate != null;
    }

    public boolean hasNumberOfPayments() {
        return numberOfPayments > 0;
    }

    public boolean isStartDateInFuture() {
        return startDate.isAfter(LocalDateTime.now());
    }

    public boolean isEndDateInFuture() {
        return endDate != null && endDate.isAfter(LocalDateTime.now());
    }

    public boolean isValidDateRange() {
        return endDate == null || startDate.isBefore(endDate);
    }

    public BigDecimal getTotalScheduledAmount() {
        if (numberOfPayments <= 0) return BigDecimal.ZERO;
        return paymentAmount.multiply(BigDecimal.valueOf(numberOfPayments));
    }

    public int getEstimatedPaymentCount() {
        if (numberOfPayments > 0) {
            return numberOfPayments;
        }
        if (endDate != null && frequency != null) {
            // Calculate based on frequency and date range
            return frequency.getPaymentsPerYear(); // Simplified calculation
        }
        return 0;
    }
}