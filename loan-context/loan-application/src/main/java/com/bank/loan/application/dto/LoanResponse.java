package com.bank.loan.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO representing loan data in responses
 */
public record LoanResponse(
    String loanId,
    String customerId,
    BigDecimal principalAmount,
    BigDecimal annualInterestRate,
    Integer termInMonths,
    String status,
    LocalDate applicationDate,
    LocalDate approvalDate,
    LocalDate disbursementDate,
    LocalDate maturityDate,
    BigDecimal outstandingBalance,
    BigDecimal monthlyPayment,
    Instant createdAt,
    Instant lastModifiedAt
) {
    
    public static LoanResponse from(com.bank.loan.domain.Loan loan) {
        return new LoanResponse(
            loan.getId().getValue(),
            loan.getCustomerId().getValue(),
            loan.getPrincipalAmount().getAmount(),
            loan.getInterestRate().getAnnualRate(),
            loan.getLoanTerm().getMonths(),
            loan.getStatus().name(),
            loan.getApplicationDate(),
            loan.getApprovalDate(),
            loan.getDisbursementDate(),
            loan.getMaturityDate(),
            loan.getOutstandingBalance().getAmount(),
            loan.calculateMonthlyPayment().getAmount(),
            loan.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant(),
            loan.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant()
        );
    }
}