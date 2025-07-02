package com.bank.loanmanagement.loan.infrastructure.repository;

import com.bank.loanmanagement.loan.domain.loan.CreditLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLoanRepository extends JpaRepository<CreditLoan, Long> {
    
    List<CreditLoan> findByCustomerId(Long customerId);
    
    List<CreditLoan> findByCustomerIdAndNumberOfInstallments(Long customerId, Integer numberOfInstallments);
    
    List<CreditLoan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    
    List<CreditLoan> findByCustomerIdAndNumberOfInstallmentsAndIsPaid(Long customerId, Integer numberOfInstallments, Boolean isPaid);
}