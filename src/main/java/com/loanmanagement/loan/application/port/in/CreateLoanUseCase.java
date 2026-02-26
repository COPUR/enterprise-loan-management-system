package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.shared.domain.model.Money;
import java.math.BigDecimal;

public interface CreateLoanUseCase {
    
    Loan createLoan(CreateLoanCommand command);
    
    record CreateLoanCommand(
            Long customerId,
            Money principalAmount,
            BigDecimal interestRate,
            Integer termMonths
    ) {}
}