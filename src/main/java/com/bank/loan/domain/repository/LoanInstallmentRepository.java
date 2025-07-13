package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.LoanInstallment;
import com.bank.loan.domain.model.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LoanInstallment entity operations
 */
@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {
    
    /**
     * Find installments by loan ID
     */
    List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
    
    /**
     * Find specific installment by loan ID and installment number
     */
    Optional<LoanInstallment> findByLoanIdAndInstallmentNumber(Long loanId, Integer installmentNumber);
    
    /**
     * Find installments by loan ID and status
     */
    List<LoanInstallment> findByLoanIdAndStatus(Long loanId, InstallmentStatus status);
    
    /**
     * Find unpaid installments for a loan
     */
    @Query("SELECT i FROM LoanInstallment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID') ORDER BY i.installmentNumber ASC")
    List<LoanInstallment> findUnpaidInstallmentsByLoanId(@Param("loanId") Long loanId);
    
    /**
     * Find next unpaid installment for a loan
     */
    @Query("SELECT i FROM LoanInstallment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID') ORDER BY i.installmentNumber ASC")
    Optional<LoanInstallment> findNextUnpaidInstallment(@Param("loanId") Long loanId);
    
    /**
     * Find overdue installments
     */
    @Query("SELECT i FROM LoanInstallment i WHERE i.dueDate < :currentDate AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID')")
    List<LoanInstallment> findOverdueInstallments(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find installments due in date range
     */
    @Query("SELECT i FROM LoanInstallment i WHERE i.dueDate BETWEEN :startDate AND :endDate AND i.status IN ('PENDING', 'DUE')")
    List<LoanInstallment> findInstallmentsDueInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Find installments due within next 3 months for a loan
     */
    @Query("SELECT i FROM LoanInstallment i WHERE i.loanId = :loanId AND i.dueDate <= :maxDate AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID') ORDER BY i.installmentNumber ASC")
    List<LoanInstallment> findInstallmentsDueWithinMonths(@Param("loanId") Long loanId, @Param("maxDate") LocalDate maxDate);
    
    /**
     * Count paid installments for a loan
     */
    @Query("SELECT COUNT(i) FROM LoanInstallment i WHERE i.loanId = :loanId AND i.status = 'PAID'")
    long countPaidInstallmentsByLoanId(@Param("loanId") Long loanId);
    
    /**
     * Count remaining installments for a loan
     */
    @Query("SELECT COUNT(i) FROM LoanInstallment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID')")
    long countRemainingInstallmentsByLoanId(@Param("loanId") Long loanId);
    
    /**
     * Sum remaining amounts for a loan
     */
    @Query("SELECT COALESCE(SUM(i.installmentAmount.amount - COALESCE(i.paidAmount.amount, 0)), 0) FROM LoanInstallment i WHERE i.loanId = :loanId AND i.status IN ('PENDING', 'DUE', 'OVERDUE', 'PARTIALLY_PAID')")
    java.math.BigDecimal sumRemainingAmountsByLoanId(@Param("loanId") Long loanId);
    
    /**
     * Find installments by payment reference
     */
    List<LoanInstallment> findByPaymentReference(String paymentReference);
    
    /**
     * Delete installments by loan ID
     */
    void deleteByLoanId(Long loanId);
}