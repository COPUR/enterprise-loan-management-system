package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing current loan balance information.
 */
@Value
@Builder
@With
public class LoanBalance {
    
    String loanId;
    BigDecimal principalBalance;
    BigDecimal interestBalance;
    BigDecimal feesBalance;
    BigDecimal penaltyBalance;
    BigDecimal escrowBalance;
    BigDecimal insuranceBalance;
    BigDecimal taxBalance;
    BigDecimal totalBalance;
    String currencyCode;
    LocalDateTime asOfDate;
    LocalDateTime nextPaymentDate;
    BigDecimal nextPaymentAmount;
    BigDecimal minimumPaymentAmount;
    BigDecimal pastDueAmount;
    int daysPastDue;
    BigDecimal originalLoanAmount;
    BigDecimal totalPaid;
    BigDecimal remainingTerm;
    Map<String, String> metadata;

    public static class LoanBalanceBuilder {
        public LoanBalanceBuilder principalBalance(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Principal balance cannot be negative");
            }
            this.principalBalance = amount;
            return this;
        }

        public LoanBalanceBuilder interestBalance(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Interest balance cannot be negative");
            }
            this.interestBalance = amount;
            return this;
        }

        public LoanBalanceBuilder totalBalance(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total balance cannot be negative");
            }
            this.totalBalance = amount;
            return this;
        }

        public LoanBalance build() {
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (asOfDate == null) {
                this.asOfDate = LocalDateTime.now();
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Set default values for null amounts
            if (principalBalance == null) this.principalBalance = BigDecimal.ZERO;
            if (interestBalance == null) this.interestBalance = BigDecimal.ZERO;
            if (feesBalance == null) this.feesBalance = BigDecimal.ZERO;
            if (penaltyBalance == null) this.penaltyBalance = BigDecimal.ZERO;
            if (escrowBalance == null) this.escrowBalance = BigDecimal.ZERO;
            if (insuranceBalance == null) this.insuranceBalance = BigDecimal.ZERO;
            if (taxBalance == null) this.taxBalance = BigDecimal.ZERO;
            if (pastDueAmount == null) this.pastDueAmount = BigDecimal.ZERO;
            if (totalPaid == null) this.totalPaid = BigDecimal.ZERO;
            if (remainingTerm == null) this.remainingTerm = BigDecimal.ZERO;
            
            // Calculate total balance if not provided
            if (totalBalance == null) {
                this.totalBalance = principalBalance.add(interestBalance).add(feesBalance)
                        .add(penaltyBalance).add(escrowBalance).add(insuranceBalance).add(taxBalance);
            }
            
            return new LoanBalance(
                loanId, principalBalance, interestBalance, feesBalance, penaltyBalance,
                escrowBalance, insuranceBalance, taxBalance, totalBalance, currencyCode,
                asOfDate, nextPaymentDate, nextPaymentAmount, minimumPaymentAmount,
                pastDueAmount, daysPastDue, originalLoanAmount, totalPaid, remainingTerm, metadata
            );
        }
    }

    public boolean isPastDue() {
        return daysPastDue > 0 || (pastDueAmount != null && pastDueAmount.compareTo(BigDecimal.ZERO) > 0);
    }

    public boolean isDelinquent() {
        return daysPastDue > 30;
    }

    public boolean isInDefault() {
        return daysPastDue > 90;
    }

    public BigDecimal getPayoffAmount() {
        return totalBalance;
    }

    public boolean isPaidOff() {
        return totalBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getPercentagePaid() {
        if (originalLoanAmount == null || originalLoanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPaid.divide(originalLoanAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}