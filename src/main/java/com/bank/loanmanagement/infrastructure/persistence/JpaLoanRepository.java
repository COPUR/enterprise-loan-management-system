package com.bank.loanmanagement.infrastructure.persistence;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaLoanRepository extends JpaRepository<Loan, LoanId> {
    
    @Query("SELECT l FROM Loan l WHERE l.customerId = :customerId")
    List<Loan> findByCustomerId(@Param("customerId") CustomerId customerId);
    
    @Query("SELECT l FROM Loan l WHERE l.status = :status")
    List<Loan> findByStatus(@Param("status") LoanStatus status);
    
    @Query("SELECT l FROM Loan l WHERE l.loanType = :type")
    List<Loan> findByType(@Param("type") LoanType type);
    
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND EXISTS " +
           "(SELECT i FROM LoanInstallment i WHERE i.loan = l AND i.dueDate < CURRENT_DATE AND i.status = 'PENDING')")
    List<Loan> findOverdueLoans();
}