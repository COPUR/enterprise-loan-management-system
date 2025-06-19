
package com.bank.loanmanagement.domain.repository;

import com.bank.loanmanagement.domain.model.Loan;
import com.bank.loanmanagement.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    List<Loan> findByCustomer(Customer customer);
    
    List<Loan> findByCustomerCustomerId(Long customerId);
    
    List<Loan> findByStatus(Loan.LoanStatus status);
    
    @Query("SELECT l FROM Loan l WHERE l.outstandingBalance > 0 AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansWithBalance();
    
    @Query("SELECT l FROM Loan l WHERE l.maturityDate < :date AND l.status = 'ACTIVE'")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);
    
    @Query("SELECT l FROM Loan l WHERE l.loanAmount BETWEEN :minAmount AND :maxAmount")
    List<Loan> findByLoanAmountBetween(@Param("minAmount") BigDecimal minAmount, 
                                     @Param("maxAmount") BigDecimal maxAmount);
    
    @Query("SELECT l FROM Loan l WHERE l.interestRate BETWEEN :minRate AND :maxRate")
    List<Loan> findByInterestRateBetween(@Param("minRate") BigDecimal minRate, 
                                       @Param("maxRate") BigDecimal maxRate);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    Long countByStatus(@Param("status") Loan.LoanStatus status);
    
    @Query("SELECT SUM(l.loanAmount) FROM Loan l WHERE l.status = 'ACTIVE'")
    BigDecimal getTotalActiveLoanAmount();
    
    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.status = 'ACTIVE'")
    BigDecimal getTotalOutstandingBalance();
    
    @Query("SELECT AVG(l.interestRate) FROM Loan l WHERE l.status = 'ACTIVE'")
    BigDecimal getAverageInterestRate();
    
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansForCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT l FROM Loan l WHERE l.startDate BETWEEN :startDate AND :endDate")
    List<Loan> findLoansByDateRange(@Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate);
}
