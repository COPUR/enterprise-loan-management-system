package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.Payment;
import com.bank.loan.domain.model.PaymentStatus;
import com.bank.loan.domain.model.LoanId;
import com.bank.loan.domain.model.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Find payment by reference
     */
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    /**
     * Find payments by loan ID
     */
    @Query("SELECT p FROM Payment p WHERE p.loanId.value = :loanId")
    List<Payment> findByLoanId(@Param("loanId") String loanId);
    
    /**
     * Find payments by customer ID
     */
    @Query("SELECT p FROM Payment p WHERE p.customerId.value = :customerId")
    List<Payment> findByCustomerId(@Param("customerId") String customerId);
    
    /**
     * Find payments by loan ID with pagination
     */
    @Query("SELECT p FROM Payment p WHERE p.loanId.value = :loanId")
    Page<Payment> findByLoanId(@Param("loanId") String loanId, Pageable pageable);
    
    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find payments by status and date range
     */
    List<Payment> findByStatusAndPaymentDateBetween(PaymentStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find pending payments older than specified date
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.paymentDate < :cutoffDate")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find successful payments for a loan
     */
    @Query("SELECT p FROM Payment p WHERE p.loanId.value = :loanId AND p.status = 'PROCESSED'")
    List<Payment> findSuccessfulPaymentsByLoanId(@Param("loanId") String loanId);
    
    /**
     * Find payments by transaction reference
     */
    Optional<Payment> findByTransactionReference(String transactionReference);
    
    /**
     * Find payments in date range
     */
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count payments by status
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Count successful payments for a loan
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.loanId.value = :loanId AND p.status = 'PROCESSED'")
    long countSuccessfulPaymentsByLoanId(@Param("loanId") String loanId);
    
    /**
     * Sum successful payment amounts for a loan
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount.amount), 0) FROM Payment p WHERE p.loanId.value = :loanId AND p.status = 'PROCESSED'")
    java.math.BigDecimal sumSuccessfulPaymentsByLoanId(@Param("loanId") String loanId);
    
    /**
     * Find payments with specific filters
     */
    @Query("SELECT p FROM Payment p WHERE " +
           "(:loanId IS NULL OR p.loanId.value = :loanId) AND " +
           "(:customerId IS NULL OR p.customerId.value = :customerId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:startDate IS NULL OR p.paymentDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.paymentDate <= :endDate)")
    List<Payment> findPaymentsWithFilters(
        @Param("loanId") String loanId,
        @Param("customerId") String customerId,
        @Param("status") PaymentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}