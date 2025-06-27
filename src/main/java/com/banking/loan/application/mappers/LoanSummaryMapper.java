package com.banking.loan.application.mappers;

import com.banking.loan.domain.loan.LoanAggregate;
import com.banking.loan.application.results.LoanSummary;

import java.time.LocalDateTime;

/**
 * Mapper for converting LoanAggregate to LoanSummary
 */
public class LoanSummaryMapper {
    
    public static LoanSummary toSummary(LoanAggregate loan) {
        return new LoanSummary(
            loan.getId().value(),
            loan.getAmount().value(),
            loan.getInterestRate().value(),
            loan.getTerm().months(),
            loan.getStatus().toString(),
            java.time.LocalDate.now(), // applicationDate - default value
            java.time.LocalDate.now(), // approvalDate - default value
            loan.getAmount().value(), // outstandingBalance - default to full amount
            null, // nextPaymentDate - would need to be calculated
            null, // nextPaymentAmount - would need to be calculated
            LocalDateTime.now() // lastModified
        );
    }
}