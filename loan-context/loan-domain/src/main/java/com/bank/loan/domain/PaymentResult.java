package com.bank.loan.domain;

import com.bank.shared.kernel.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object representing the result of a loan payment operation
 * 
 * This immutable value object encapsulates the complete result of
 * processing a payment, including the updated loan state and
 * transaction details.
 * 
 * GRASP Principles Applied:
 * - Information Expert: Contains complete payment processing results
 * - Low Coupling: Self-contained result object
 * - High Cohesion: All data related to payment processing outcome
 */
@Value
@Builder
public class PaymentResult {
    
    /**
     * ID of the loan that received the payment
     */
    LoanId loanId;
    
    /**
     * Unique identifier for this payment transaction
     */
    PaymentId paymentId;
    
    /**
     * Detailed breakdown of how the payment was distributed
     */
    PaymentDistribution paymentDistribution;
    
    /**
     * Outstanding loan balance after this payment
     */
    Money newOutstandingBalance;
    
    /**
     * Loan status after payment processing
     */
    LoanStatus loanStatus;
    
    /**
     * Timestamp when payment was processed
     */
    LocalDateTime paymentProcessedAt;
    
    /**
     * Whether the payment was processed successfully
     */
    boolean success;
    
    /**
     * Error message if payment failed
     */
    String errorMessage;
    
    /**
     * Additional processing notes or warnings
     */
    String processingNotes;

    /**
     * Check if the payment was processed successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the loan is now fully paid after this payment
     */
    public boolean isLoanFullyPaid() {
        return newOutstandingBalance != null && 
               newOutstandingBalance.isZero() && 
               loanStatus == LoanStatus.FULLY_PAID;
    }

    /**
     * Get the amount that was actually applied to reduce the loan balance
     */
    public Money getBalanceReduction() {
        if (paymentDistribution == null) {
            return Money.aed(java.math.BigDecimal.ZERO);
        }
        
        Money previousBalance = paymentDistribution.getPreviousBalance();
        
        if (previousBalance == null || newOutstandingBalance == null) {
            return Money.aed(java.math.BigDecimal.ZERO);
        }
        
        return previousBalance.subtract(newOutstandingBalance);
    }

    /**
     * Check if payment processing encountered any issues
     */
    public boolean hasWarnings() {
        return processingNotes != null && !processingNotes.trim().isEmpty();
    }

    /**
     * Check if payment failed
     */
    public boolean hasFailed() {
        return !success;
    }

    /**
     * Get a human-readable summary of the payment result
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (success) {
            summary.append("✓ Payment Processed Successfully\n");
            summary.append(String.format("Payment ID: %s\n", paymentId));
            summary.append(String.format("Loan ID: %s\n", loanId));
            summary.append(String.format("Amount Paid: %s AED\n", 
                                        paymentDistribution.getTotalPayment().getAmount()));
            summary.append(String.format("New Balance: %s AED\n", newOutstandingBalance.getAmount()));
            summary.append(String.format("Loan Status: %s\n", loanStatus));
            
            if (isLoanFullyPaid()) {
                summary.append("🎉 Congratulations! Your loan is now fully paid!\n");
            }
            
            if (hasWarnings()) {
                summary.append(String.format("⚠️  Note: %s\n", processingNotes));
            }
        } else {
            summary.append("❌ Payment Processing Failed\n");
            summary.append(String.format("Error: %s\n", errorMessage));
        }
        
        summary.append(String.format("Processed at: %s\n", paymentProcessedAt));
        
        return summary.toString();
    }

    /**
     * Get detailed payment breakdown information
     */
    public String getDetailedBreakdown() {
        if (!success || paymentDistribution == null) {
            return "Payment breakdown not available - processing failed";
        }
        
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("PAYMENT BREAKDOWN DETAILS\n");
        breakdown.append("========================\n");
        breakdown.append(paymentDistribution.getSummary());
        breakdown.append("\nLOAN STATUS AFTER PAYMENT\n");
        breakdown.append("=========================\n");
        breakdown.append(String.format("Outstanding Balance: %s AED\n", newOutstandingBalance.getAmount()));
        breakdown.append(String.format("Loan Status: %s\n", loanStatus));
        breakdown.append(String.format("Balance Reduction: %s AED\n", getBalanceReduction().getAmount()));
        
        return breakdown.toString();
    }

    /**
     * Create a successful payment result
     */
    public static PaymentResult success(LoanId loanId, PaymentId paymentId, 
                                       PaymentDistribution distribution,
                                       Money newBalance, LoanStatus newStatus) {
        PaymentResult result = PaymentResult.builder()
            .loanId(loanId)
            .paymentId(paymentId)
            .paymentDistribution(distribution)
            .newOutstandingBalance(newBalance)
            .loanStatus(newStatus)
            .paymentProcessedAt(LocalDateTime.now())
            .success(true)
            .build();
        result.validate();
        return result;
    }

    /**
     * Create a failed payment result
     */
    public static PaymentResult failure(LoanId loanId, String errorMessage) {
        PaymentResult result = PaymentResult.builder()
            .loanId(loanId)
            .paymentProcessedAt(LocalDateTime.now())
            .success(false)
            .errorMessage(errorMessage)
            .build();
        result.validate();
        return result;
    }

    /**
     * Create a successful payment result with warnings
     */
    public static PaymentResult successWithWarnings(LoanId loanId, PaymentId paymentId,
                                                   PaymentDistribution distribution,
                                                   Money newBalance, LoanStatus newStatus,
                                                   String warnings) {
        PaymentResult result = PaymentResult.builder()
            .loanId(loanId)
            .paymentId(paymentId)
            .paymentDistribution(distribution)
            .newOutstandingBalance(newBalance)
            .loanStatus(newStatus)
            .paymentProcessedAt(LocalDateTime.now())
            .success(true)
            .processingNotes(warnings)
            .build();
        result.validate();
        return result;
    }

    /**
     * Validate the payment result is consistent
     */
    private void validateResult() {
        Objects.requireNonNull(loanId, "Loan ID cannot be null");
        Objects.requireNonNull(paymentProcessedAt, "Payment processed timestamp cannot be null");
        
        if (success) {
            Objects.requireNonNull(paymentId, "Payment ID required for successful payments");
            Objects.requireNonNull(paymentDistribution, "Payment distribution required for successful payments");
            Objects.requireNonNull(newOutstandingBalance, "New balance required for successful payments");
            Objects.requireNonNull(loanStatus, "Loan status required for successful payments");
            
            if (!paymentDistribution.isValid()) {
                throw new IllegalArgumentException("Payment distribution is invalid");
            }
        } else {
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message required for failed payments");
            }
        }
    }

    /**
     * Validate the payment result after construction
     */
    public void validate() {
        validateResult();
    }
}