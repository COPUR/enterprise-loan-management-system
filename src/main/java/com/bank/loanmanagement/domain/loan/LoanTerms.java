package com.bank.loanmanagement.domain.loan;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanTerms {
    
    private BigDecimal penaltyRate;
    private BigDecimal lateFeeAmount;
    private Integer gracePeriodDays;
    private Boolean allowPrepayment;
    private BigDecimal prepaymentPenaltyRate;
    private String specialConditions;
    
    public static LoanTerms standard() {
        return new LoanTerms(
            BigDecimal.valueOf(0.02), // 2% penalty rate
            BigDecimal.valueOf(25.00), // $25 late fee
            5, // 5 days grace period
            true, // Allow prepayment
            BigDecimal.ZERO, // No prepayment penalty
            "Standard loan terms and conditions"
        );
    }
}