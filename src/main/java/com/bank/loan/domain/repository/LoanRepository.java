package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.Loan;
import com.bank.loan.domain.model.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Loan entity operations
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    /**
     * Find loans by customer ID
     */
    List<Loan> findByCustomerId(Long customerId);
    
    /**
     * Find loans by customer ID with pagination
     */
    Page<Loan> findByCustomerId(Long customerId, Pageable pageable);
    
    /**
     * Find loans by customer ID and status
     */
    List<Loan> findByCustomerIdAndStatus(Long customerId, LoanStatus status);
    
    /**
     * Find loans by customer ID and number of installments
     */
    List<Loan> findByCustomerIdAndTermMonths(Long customerId, Integer termMonths);
    
    /**
     * Find loans by customer ID and paid status
     */
    List<Loan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    
    /**
     * Find loan by reference number
     */
    Optional<Loan> findByLoanReference(String loanReference);
    
    /**
     * Find active loans (not fully paid or closed)
     */
    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED', 'APPROVED') AND l.isPaid = false")
    List<Loan> findActiveLoan();
    
    /**
     * Find overdue loans
     */
    @Query("SELECT DISTINCT l FROM Loan l JOIN l.installments i WHERE i.dueDate < :currentDate AND i.status IN ('PENDING', 'DUE', 'OVERDUE') AND l.isPaid = false")
    List<Loan> findOverdueLoans(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find loans by status
     */
    List<Loan> findByStatus(LoanStatus status);
    
    /**
     * Find loans due for payment in date range
     */
    @Query("SELECT DISTINCT l FROM Loan l JOIN l.installments i WHERE i.dueDate BETWEEN :startDate AND :endDate AND i.status IN ('PENDING', 'DUE')")
    List<Loan> findLoansDueInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Count loans by customer
     */
    long countByCustomerId(Long customerId);
    
    /**
     * Count active loans by customer
     */
    long countByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    
    /**
     * Find loans with specific filters
     */
    @Query("SELECT l FROM Loan l WHERE " +
           "(:customerId IS NULL OR l.customerId = :customerId) AND " +
           "(:termMonths IS NULL OR l.termMonths = :termMonths) AND " +
           "(:isPaid IS NULL OR l.isPaid = :isPaid) AND " +
           "(:status IS NULL OR l.status = :status)")
    List<Loan> findLoansWithFilters(
        @Param("customerId") Long customerId,
        @Param("termMonths") Integer termMonths,
        @Param("isPaid") Boolean isPaid,
        @Param("status") LoanStatus status
    );
}