package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Value object representing payment allocation details.
 */
@Value
@Builder
@With
public class PaymentAllocation {
    
    String allocationId;
    String loanId;
    String paymentId;
    PaymentAllocationStrategy strategy;
    BigDecimal totalAmount;
    BigDecimal principalAmount;
    BigDecimal interestAmount;
    BigDecimal feesAmount;
    BigDecimal penaltyAmount;
    BigDecimal escrowAmount;
    BigDecimal insuranceAmount;
    BigDecimal taxAmount;
    BigDecimal remainingAmount;
    String currencyCode;
    List<AllocationDetail> details;
    Map<String, String> metadata;

    @Value
    @Builder
    @With
    public static class AllocationDetail {
        String componentType;
        String componentId;
        BigDecimal allocatedAmount;
        BigDecimal remainingBalance;
        String description;
        Map<String, String> metadata;
    }

    public static class PaymentAllocationBuilder {
        public PaymentAllocationBuilder totalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total amount cannot be negative");
            }
            this.totalAmount = amount;
            return this;
        }

        public PaymentAllocationBuilder principalAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Principal amount cannot be negative");
            }
            this.principalAmount = amount;
            return this;
        }

        public PaymentAllocationBuilder interestAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Interest amount cannot be negative");
            }
            this.interestAmount = amount;
            return this;
        }

        public PaymentAllocationBuilder feesAmount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fees amount cannot be negative");
            }
            this.feesAmount = amount;
            return this;
        }

        public PaymentAllocation build() {
            if (allocationId == null || allocationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Allocation ID is required");
            }
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (totalAmount == null) {
                throw new IllegalArgumentException("Total amount is required");
            }
            if (strategy == null) {
                this.strategy = PaymentAllocationStrategy.INTEREST_FIRST;
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (details == null) {
                this.details = List.of();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Set default values for null amounts
            if (principalAmount == null) this.principalAmount = BigDecimal.ZERO;
            if (interestAmount == null) this.interestAmount = BigDecimal.ZERO;
            if (feesAmount == null) this.feesAmount = BigDecimal.ZERO;
            if (penaltyAmount == null) this.penaltyAmount = BigDecimal.ZERO;
            if (escrowAmount == null) this.escrowAmount = BigDecimal.ZERO;
            if (insuranceAmount == null) this.insuranceAmount = BigDecimal.ZERO;
            if (taxAmount == null) this.taxAmount = BigDecimal.ZERO;
            if (remainingAmount == null) this.remainingAmount = BigDecimal.ZERO;
            
            return new PaymentAllocation(
                allocationId, loanId, paymentId, strategy, totalAmount,
                principalAmount, interestAmount, feesAmount, penaltyAmount,
                escrowAmount, insuranceAmount, taxAmount, remainingAmount,
                currencyCode, details, metadata
            );
        }
    }

    public BigDecimal getAllocatedAmount() {
        return principalAmount.add(interestAmount).add(feesAmount).add(penaltyAmount)
                .add(escrowAmount).add(insuranceAmount).add(taxAmount);
    }

    public boolean isFullyAllocated() {
        return getAllocatedAmount().compareTo(totalAmount) == 0;
    }

    public BigDecimal getUnallocatedAmount() {
        return totalAmount.subtract(getAllocatedAmount());
    }

    public boolean hasUnallocatedAmount() {
        return getUnallocatedAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}