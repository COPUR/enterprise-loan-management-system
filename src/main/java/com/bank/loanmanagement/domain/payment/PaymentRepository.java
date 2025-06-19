package com.bank.loanmanagement.domain.payment;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.LoanId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(PaymentId id);
    
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    List<Payment> findByLoanId(LoanId loanId);
    
    List<Payment> findByCustomerId(CustomerId customerId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Payment> findAll();
    
    void delete(Payment payment);
}