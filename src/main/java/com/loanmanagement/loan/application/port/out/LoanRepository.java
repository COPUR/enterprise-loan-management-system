package com.loanmanagement.loan.application.port.out;

import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanStatus;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    
    Loan save(Loan loan);
    
    Optional<Loan> findById(Long id);
    
    List<Loan> findByCustomerId(Long customerId);
    
    List<Loan> findByStatus(LoanStatus status);
    
    void deleteById(Long id);
}