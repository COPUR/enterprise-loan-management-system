package com.bank.loanmanagement.infrastructure.persistence;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.payment.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, PaymentId> {
    
    @Query("SELECT p FROM Payment p WHERE p.paymentReference = :paymentReference")
    Optional<Payment> findByPaymentReference(@Param("paymentReference") String paymentReference);
    
    @Query("SELECT p FROM Payment p WHERE p.loanId = :loanId")
    List<Payment> findByLoanId(@Param("loanId") LoanId loanId);
    
    @Query("SELECT p FROM Payment p WHERE p.customerId = :customerId")
    List<Payment> findByCustomerId(@Param("customerId") CustomerId customerId);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByPaymentDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}