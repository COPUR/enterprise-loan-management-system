package com.banking.loan.application.mappers;

import com.banking.loan.domain.loan.LoanAggregate;
import com.banking.loan.application.results.LoanDetails;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Mapper for converting LoanAggregate to LoanDetails
 */
public class LoanDetailsMapper {
    
    public static LoanDetails toDetails(LoanAggregate loan) {
        return new LoanDetails(
            loan.getId().value(),
            loan.getCustomerId().value(),
            loan.getAmount().value(),
            loan.getAmount().value(), // remainingAmount - default to full amount
            loan.getStatus().toString(),
            loan.getType() != null ? loan.getType().toString() : "PERSONAL", // loanType
            loan.getTerm().months(),
            LocalDateTime.now(), // createdAt
            LocalDateTime.now(), // approvedAt - default value
            Collections.emptyList() // installments - would need to be retrieved separately
        );
    }
}