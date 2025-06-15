
package com.bank.loanmanagement.domain.repository;

import com.bank.loanmanagement.domain.model.Payment;
import com.bank.loanmanagement.domain.model.Loan;
import com.bank.loanmanagement.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByLoan(Loan loan);
    
    List<Payment> findByCustomer(Customer customer);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);
    
    Optional<Payment> findByTransactionReference(String transactionReference);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.loan.loanId = :loanId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsForLoan(@Param("loanId") Long loanId);
    
    @Query("SELECT p FROM Payment p WHERE p.customer.customerId = :customerId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsForCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT SUM(p.paymentAmount) FROM Payment p WHERE p.loan.loanId = :loanId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaymentsForLoan(@Param("loanId") Long loanId);
    
    @Query("SELECT SUM(p.paymentAmount) FROM Payment p WHERE p.customer.customerId = :customerId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaymentsForCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT SUM(p.paymentAmount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaymentAmountByDateRange(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentAmount >= :minAmount")
    List<Payment> findByPaymentAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount);
    
    boolean existsByTransactionReference(String transactionReference);
}
