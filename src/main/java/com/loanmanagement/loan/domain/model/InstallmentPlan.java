package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Base installment plan domain model representing the core structure
 * of loan installment arrangements.
 */
@Data
@Builder
@With
public class InstallmentPlan {
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final String planId;
    
    @NotNull
    private final InstallmentPlanType planType;
    
    @NotNull
    @Positive
    private final BigDecimal principalAmount;
    
    @NotNull
    @PositiveOrZero
    private final BigDecimal interestRate;
    
    @NotNull
    @Positive
    private final Integer termInMonths;
    
    @NotNull
    private final PaymentFrequency paymentFrequency;
    
    @NotNull
    private final LocalDate startDate;
    
    @NotNull
    private final LocalDate endDate;
    
    @NotNull
    private final BigDecimal totalPaymentAmount;
    
    @NotNull
    private final BigDecimal totalInterestAmount;
    
    @NotNull
    private final List<ScheduledInstallment> installments;
    
    private final String description;
    
    private final boolean isActive;
    
    private final LocalDate createdDate;
    
    private final LocalDate lastModifiedDate;
    
    /**
     * Calculates the monthly payment amount for this installment plan.
     */
    public BigDecimal calculateMonthlyPayment() {
        if (installments == null || installments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return installments.stream()
                .map(ScheduledInstallment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(installments.size()), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Gets the remaining balance after a specific installment number.
     */
    public BigDecimal getRemainingBalance(int installmentNumber) {
        return installments.stream()
                .filter(inst -> inst.getInstallmentNumber() > installmentNumber)
                .map(ScheduledInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Validates the installment plan structure.
     */
    public boolean isValid() {
        return loanId != null && 
               planId != null && 
               principalAmount != null && 
               principalAmount.compareTo(BigDecimal.ZERO) > 0 &&
               termInMonths != null && 
               termInMonths > 0 &&
               installments != null && 
               !installments.isEmpty();
    }
    
    public enum InstallmentPlanType {
        STANDARD,
        VARIABLE_RATE,
        BALLOON,
        GRADUATED,
        INTEREST_ONLY,
        MODIFIED,
        CUSTOM
    }
}